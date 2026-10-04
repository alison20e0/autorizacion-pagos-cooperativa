package com.cooperativa.pagos.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.cooperativa.pagos.domain.Auditoria;
import com.cooperativa.pagos.domain.Pago;
import com.cooperativa.pagos.dto.PagoRequest;
import com.cooperativa.pagos.dto.PagoResponse;
import com.cooperativa.pagos.exception.IdempotencyKeyRequeridaException;
import com.cooperativa.pagos.repository.AuditoriaRepository;
import com.cooperativa.pagos.repository.PagoRepository;
import com.cooperativa.pagos.saldo.ServicioSaldo;

/**
 * Autoriza pagos contra el saldo del socio garantizando que una misma
 * X-Idempotency-Key nunca produzca mas de un cargo.
 *
 * La proteccion se apoya en la restriccion de unicidad sobre
 * pago.idempotency_key, no solo en una lectura previa: dos peticiones
 * concurrentes con la misma clave compiten por el INSERT y solo una gana.
 * El descuento del saldo ocurre exclusivamente despues de ganar ese INSERT,
 * de modo que una peticion perdedora nunca descuenta.
 */
@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);
    private static final int MAX_LUNGITUD_CLAVE = 128;

    private final PagoRepository pagoRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final ServicioSaldo servicioSaldo;

    public PagoService(PagoRepository pagoRepository,
                       AuditoriaRepository auditoriaRepository,
                       ServicioSaldo servicioSaldo) {
        this.pagoRepository = pagoRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.servicioSaldo = servicioSaldo;
    }

    public PagoResponse autorizar(String idempotencyKey, PagoRequest request, MetadatosRequest metadatos) {
        String clave = normalizarClave(idempotencyKey);

        Optional<Pago> previo = pagoRepository.findByIdempotencyKey(clave);
        if (previo.isPresent()) {
            return replicar(previo.get(), clave, metadatos);
        }

        auditoriaRepository.save(Auditoria.registrar(
                Auditoria.Evento.SOLICITUD_RECIBIDA, clave, null,
                "Pago solicitado por " + request.numeroSocio() + " por " + request.monto(),
                metadatos.usuario(), metadatos.direccionIp()));

        Pago pago;
        try {
            pago = procesar(clave, request, metadatos);
        } catch (DataIntegrityViolationException excepcion) {
            Optional<Pago> ganador = pagoRepository.findByIdempotencyKey(clave);
            if (ganador.isEmpty()) {
                throw excepcion;
            }
            log.info("Carrera resuelta por restriccion de unicidad para la clave {}", clave);
            return replicar(ganador.get(), clave, metadatos);
        }

        return PagoResponse.desde(pago, false);
    }

    private Pago procesar(String clave, PagoRequest request, MetadatosRequest metadatos) {
        BigDecimal saldoAntes = servicioSaldo.consultarSaldo(request.numeroSocio());

        if (saldoAntes.compareTo(request.monto()) < 0) {
            Pago rechazado = Pago.rechazar(clave, request.numeroSocio(), request.monto(),
                    request.referencia(),
                    "Saldo insuficiente: disponible " + saldoAntes, saldoAntes);
            Pago persistido = guardarYAuditar(rechazado, Auditoria.Evento.PAGO_RECHAZADO,
                    "Rechazado por saldo insuficiente", metadatos);
            return persistido;
        }

        Pago autorizado = Pago.autorizar(clave, request.numeroSocio(), request.monto(),
                request.referencia(), saldoAntes, saldoAntes);

        // El INSERT reserva la clave de idempotencia; solo quien lo gana descuenta.
        Pago persistido = pagoRepository.saveAndFlush(autorizado);

        BigDecimal saldoDespues = saldoAntes.subtract(request.monto());
        servicioSaldo.descontar(request.numeroSocio(), request.monto());

        persistido.registrarSaldoDespues(saldoDespues);
        pagoRepository.save(persistido);

        auditoriaRepository.save(Auditoria.registrar(Auditoria.Evento.PAGO_AUTORIZADO, clave,
                persistido.getId(), "Autorizado por " + request.monto(), metadatos.usuario(),
                metadatos.direccionIp()));

        return persistido;
    }

    private Pago guardarYAuditar(Pago pago, Auditoria.Evento evento, String detalle,
                                 MetadatosRequest metadatos) {
        Pago persistido = pagoRepository.saveAndFlush(pago);
        auditoriaRepository.save(Auditoria.registrar(evento, persistido.getIdempotencyKey(),
                persistido.getId(), detalle, metadatos.usuario(), metadatos.direccionIp()));
        return persistido;
    }

    private PagoResponse replicar(Pago previo, String clave, MetadatosRequest metadatos) {
        log.info("Pago {} ya procesado; se devuelve el resultado almacenado", previo.getId());
        auditoriaRepository.save(Auditoria.registrar(Auditoria.Evento.CONFLICTO_IDEMPOTENCIA, clave,
                previo.getId(), "Reutilizacion de clave; respuesta replicada sin nuevo cargo",
                metadatos.usuario(), metadatos.direccionIp()));
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