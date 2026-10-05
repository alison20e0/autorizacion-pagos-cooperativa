package com.cooperativa.pagos.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cooperativa.pagos.amqp.PagoProcesadoPublisher;
import com.cooperativa.pagos.banco.BancoRespuesta;
import com.cooperativa.pagos.banco.ServicioBanco;
import com.cooperativa.pagos.config.LimitesConfig;
import com.cooperativa.pagos.domain.Auditoria;
import com.cooperativa.pagos.domain.Cuenta;
import com.cooperativa.pagos.domain.EstadoPago;
import com.cooperativa.pagos.domain.Pago;
import com.cooperativa.pagos.dto.PagoRequest;
import com.cooperativa.pagos.dto.PagoResponse;
import com.cooperativa.pagos.exception.IdempotencyKeyRequeridaException;
import com.cooperativa.pagos.exception.LimiteExcedidoException;
import com.cooperativa.pagos.exception.PagoNoEncontradoException;
import com.cooperativa.pagos.repository.AuditoriaRepository;
import com.cooperativa.pagos.repository.CuentaRepository;
import com.cooperativa.pagos.repository.PagoRepository;

@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);
    private static final int MAX_LUNGITUD_CLAVE = 128;
    private static final int TIMEOUT_BANCO_SEGUNDOS = 3;

    private final PagoRepository pagoRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final CuentaRepository cuentaRepository;
    private final ServicioBanco servicioBanco;
    private final PagoProcesadoPublisher publisher;
    private final LimitesConfig limitesConfig;

    public PagoService(PagoRepository pagoRepository,
                       AuditoriaRepository auditoriaRepository,
                       CuentaRepository cuentaRepository,
                       ServicioBanco servicioBanco,
                       PagoProcesadoPublisher publisher,
                       LimitesConfig limitesConfig) {
        this.pagoRepository = pagoRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.cuentaRepository = cuentaRepository;
        this.servicioBanco = servicioBanco;
        this.publisher = publisher;
        this.limitesConfig = limitesConfig;
    }

    @Transactional
    public PagoResponse autorizar(String idempotencyKey, PagoRequest request, MetadatosRequest metadatos) {
        String clave = normalizarClave(idempotencyKey);
        Optional<Pago> previo = pagoRepository.findByIdempotencyKey(clave);
        if (previo.isPresent()) {
            auditoriaRepository.save(Auditoria.registrar(Auditoria.Evento.CONFLICTO_IDEMPOTENCIA, clave,
                    previo.get().getId(), "Reutilizacion de clave", metadatos.usuario(), metadatos.direccionIp()));
            return replicar(previo.get(), clave, metadatos);
        }
        auditoriaRepository.save(Auditoria.registrar(Auditoria.Evento.SOLICITUD_RECIBIDA, clave, null,
                "Solicitud recibida", metadatos.usuario(), metadatos.direccionIp()));
        try {
            return procesar(clave, request, metadatos);
        } catch (DataIntegrityViolationException e) {
            Optional<Pago> ganador = pagoRepository.findByIdempotencyKey(clave);
            if (ganador.isEmpty()) {
                auditoriaRepository.save(Auditoria.registrar(Auditoria.Evento.INTENTO_FALLIDO, clave, null,
                        "Fallo", metadatos.usuario(), metadatos.direccionIp()));
                throw e;
            }
            return replicar(ganador.get(), clave, metadatos);
        } catch (Exception e) {
            auditoriaRepository.save(Auditoria.registrar(Auditoria.Evento.FALLO_GENERAL, clave, null,
                    e.getMessage(), metadatos.usuario(), metadatos.direccionIp()));
            throw e;
        }
    }

    private PagoResponse procesar(String clave, PagoRequest request, MetadatosRequest metadatos) {
        Cuenta cuenta = cuentaRepository.findById(request.numeroSocio())
                .orElseThrow(() -> new LimiteExcedidoException("Cuenta", "Cuenta no encontrada: " + request.numeroSocio()));
        BigDecimal saldoAntes = cuenta.getSaldo();
        if (saldoAntes.compareTo(request.monto()) < 0) {
            Pago rechazado = Pago.rechazar(clave, request.numeroSocio(), request.monto(),
                    request.referencia(), "Saldo insuficiente: disponible " + saldoAntes, saldoAntes);
            Pago persistido = guardarYAuditar(rechazado, Auditoria.Evento.PAGO_RECHAZADO,
                    "Rechazado por saldo insuficiente", metadatos);
            publisher.publicar(persistido);
            return PagoResponse.desde(persistido, false);
        }
        BigDecimal limitePago = limitesConfig.getLimitePago();
        if (limitePago != null && request.monto().compareTo(limitePago) > 0) {
            Pago rechazado = Pago.rechazar(clave, request.numeroSocio(), request.monto(),
                    request.referencia(), "Límite por pago excedido", saldoAntes);
            Pago persistido = guardarYAuditar(rechazado, Auditoria.Evento.PAGO_RECHAZADO,
                    "Rechazado por límite por pago excedido", metadatos);
            publisher.publicar(persistido);
            return PagoResponse.desde(persistido, false);
        }
        BigDecimal gastoDiario = cuenta.getGastoDiario() != null ? cuenta.getGastoDiario() : BigDecimal.ZERO;
        BigDecimal nuevoGastoDiario = gastoDiario.add(request.monto());
        BigDecimal limiteDiario = limitesConfig.getLimiteDiario();
        if (limiteDiario != null && nuevoGastoDiario.compareTo(limiteDiario) > 0) {
            Pago rechazado = Pago.rechazar(clave, request.numeroSocio(), request.monto(),
                    request.referencia(), "Límite diario excedido", saldoAntes);
            Pago persistido = guardarYAuditar(rechazado, Auditoria.Evento.PAGO_RECHAZADO,
                    "Rechazado por límite diario excedido", metadatos);
            publisher.publicar(persistido);
            return PagoResponse.desde(persistido, false);
        }
        CompletableFuture<BancoRespuesta> futureBanco = servicioBanco.autorizarPago(
                request.numeroSocio(), request.monto(), request.referencia());
        try {
            BancoRespuesta respuestaBanco = futureBanco.get(TIMEOUT_BANCO_SEGUNDOS, TimeUnit.SECONDS);
            if (respuestaBanco.error() || !respuestaBanco.autorizado()) {
                Pago rechazado = Pago.rechazar(clave, request.numeroSocio(), request.monto(),
                        request.referencia(), respuestaBanco.motivo() != null ? respuestaBanco.motivo() : "Rechazado por banco", saldoAntes);
                Pago persistido = guardarYAuditar(rechazado, Auditoria.Evento.BANCO_FALLIDO,
                        "Rechazo desde banco", metadatos);
                publisher.publicar(persistido);
                return PagoResponse.desde(persistido, false);
            }
            Pago autorizado = Pago.autorizar(clave, request.numeroSocio(), request.monto(),
                    request.referencia(), saldoAntes, saldoAntes);
            Pago persistido = pagoRepository.saveAndFlush(autorizado);
            BigDecimal saldoDespues = saldoAntes.subtract(request.monto());
            cuenta.debitar(request.monto());
            cuentaRepository.save(cuenta);
            persistido.registrarSaldoDespues(saldoDespues);
            pagoRepository.save(persistido);
            auditoriaRepository.save(Auditoria.registrar(Auditoria.Evento.PAGO_AUTORIZADO, clave,
                    persistido.getId(), "Autorizado por " + request.monto(), metadatos.usuario(),
                    metadatos.direccionIp()));
            publisher.publicar(persistido);
            return PagoResponse.desde(persistido, false);
        } catch (TimeoutException te) {
            Pago pendiente = Pago.rechazar(clave, request.numeroSocio(), request.monto(),
                    request.referencia(), "Timeout al esperar respuesta del banco (3s)", saldoAntes);
            pendiente.establecerEstado(EstadoPago.PENDIENTE_BANCO);
            Pago persistido = pagoRepository.saveAndFlush(pendiente);
            auditoriaRepository.save(Auditoria.registrar(Auditoria.Evento.BANCO_TIMEOUT,
                    persistido.getIdempotencyKey(), persistido.getId(),
                    "Timeout esperando respuesta del banco tras 3s", metadatos.usuario(), metadatos.direccionIp()));
            auditoriaRepository.save(Auditoria.registrar(Auditoria.Evento.PAGO_PENDIENTE_BANCO,
                    persistido.getIdempotencyKey(), persistido.getId(),
                    "Pago queda en PENDIENTE_BANCO por timeout del banco", metadatos.usuario(), metadatos.direccionIp()));
            publisher.publicar(persistido);
            return PagoResponse.desde(persistido, false);
        } catch (Exception e) {
            Pago rechazado = Pago.rechazar(clave, request.numeroSocio(), request.monto(),
                    request.referencia(), "Error al comunicarse con banco", saldoAntes);
            Pago persistido = guardarYAuditar(rechazado, Auditoria.Evento.BANCO_FALLIDO,
                    "Fallo comunicacion banco", metadatos);
            publisher.publicar(persistido);
            return PagoResponse.desde(persistido, false);
        }
    }

    @Transactional(readOnly = true)
    public PagoResponse obtenerPorId(String id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (Exception e) {
            throw new PagoNoEncontradoException();
        }
        Pago pago = pagoRepository.findById(uuid).orElseThrow(PagoNoEncontradoException::new);
        return PagoResponse.desde(pago, false);
    }

    private Pago guardarYAuditar(Pago pago, Auditoria.Evento evento, String detalle, MetadatosRequest metadatos) {
        Pago persistido = pagoRepository.saveAndFlush(pago);
        auditoriaRepository.save(Auditoria.registrar(evento, persistido.getIdempotencyKey(),
                persistido.getId(), detalle, metadatos.usuario(), metadatos.direccionIp()));
        return persistido;
    }

    private PagoResponse replicar(Pago previo, String clave, MetadatosRequest metadatos) {
        log.info("Pago ya procesado");
        return PagoResponse.desde(previo, true);
    }

    private String normalizarClave(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IdempotencyKeyRequeridaException();
        }
        String clave = idempotencyKey.trim();
        if (clave.length() > MAX_LUNGITUD_CLAVE) {
            throw new IllegalArgumentException(
                    "X-Idempotency-Key no debe exceder " + MAX_LUNGITUD_CLAVE + " caracteres");
        }
        return clave;
    }
}
