package com.cooperativa.pagos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cooperativa.pagos.amqp.PagoProcesadoPublisher;
import com.cooperativa.pagos.banco.BancoRespuesta;
import com.cooperativa.pagos.banco.ServicioBanco;
import com.cooperativa.pagos.config.LimitesConfig;
import com.cooperativa.pagos.domain.Cuenta;
import com.cooperativa.pagos.domain.EstadoPago;
import com.cooperativa.pagos.domain.Pago;
import com.cooperativa.pagos.dto.PagoRequest;
import com.cooperativa.pagos.dto.PagoResponse;
import com.cooperativa.pagos.exception.IdempotencyKeyRequeridaException;
import com.cooperativa.pagos.repository.AuditoriaRepository;
import com.cooperativa.pagos.repository.CuentaRepository;
import com.cooperativa.pagos.repository.PagoRepository;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private AuditoriaRepository auditoriaRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private ServicioBanco servicioBanco;

    @Mock
    private PagoProcesadoPublisher publisher;

    private LimitesConfig limitesConfig;
    private PagoService pagoService;
    private MetadatosRequest metadatos;

    @BeforeEach
    void setUp() {
        limitesConfig = new LimitesConfig();
        limitesConfig.setLimitePago(new BigDecimal("1000.00"));
        limitesConfig.setLimiteDiario(new BigDecimal("5000.00"));
        pagoService = new PagoService(pagoRepository, auditoriaRepository, cuentaRepository, servicioBanco, publisher,
                limitesConfig);
        metadatos = new MetadatosRequest("usuario-test", "127.0.0.1");
        when(pagoRepository.saveAndFlush(any(Pago.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
        when(pagoRepository.save(any(Pago.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void rechazaPeticionSinIdempotencyKey() {
        PagoRequest request = new PagoRequest("S-1001", new BigDecimal("10.00"), "REF-1");
        assertThatThrownBy(() -> pagoService.autorizar("  ", request, metadatos))
                .isInstanceOf(IdempotencyKeyRequeridaException.class);
        verify(pagoRepository, never()).saveAndFlush(any());
    }

    @Test
    void autorizaYDescuentaElSaldo() {
        PagoRequest request = new PagoRequest("S-1001", new BigDecimal("100.00"), "REF-2");
        when(pagoRepository.findByIdempotencyKey("clave-1")).thenReturn(Optional.empty());
        when(cuentaRepository.findById("S-1001"))
                .thenReturn(Optional.of(new Cuenta("S-1001", new BigDecimal("500.00"), new BigDecimal("1000.00"),
                        new BigDecimal("5000.00"), BigDecimal.ZERO)));
        when(servicioBanco.autorizarPago(anyString(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new BancoRespuesta(true, "ok", false)));

        PagoResponse respuesta = pagoService.autorizar("clave-1", request, metadatos);

        assertThat(respuesta.estado()).isEqualTo(EstadoPago.AUTORIZADO);
        assertThat(respuesta.replicado()).isFalse();
        assertThat(respuesta.saldoDespues()).isEqualByComparingTo("400.00");
        verify(cuentaRepository, times(1)).save(any(Cuenta.class));
    }

    @Test
    void rechazaSinDescontarCuandoElSaldoNoAlcanza() {
        PagoRequest request = new PagoRequest("S-1003", new BigDecimal("500.00"), "REF-3");
        when(pagoRepository.findByIdempotencyKey("clave-2")).thenReturn(Optional.empty());
        when(cuentaRepository.findById("S-1003"))
                .thenReturn(Optional.of(new Cuenta("S-1003", new BigDecimal("80.00"), new BigDecimal("1000.00"),
                        new BigDecimal("5000.00"), BigDecimal.ZERO)));

        PagoResponse respuesta = pagoService.autorizar("clave-2", request, metadatos);

        assertThat(respuesta.estado()).isEqualTo(EstadoPago.RECHAZADO);
        assertThat(respuesta.motivoRechazo()).contains("Saldo insuficiente");
        verify(cuentaRepository, never()).save(any(Cuenta.class));
    }

    @Test
    void replicaElPagoPrevioSinGenerarUnSegundoCargo() {
        PagoRequest request = new PagoRequest("S-1001", new BigDecimal("100.00"), "REF-4");
        Pago previo = Pago.autorizar("clave-3", "S-1001", new BigDecimal("100.00"), "REF-4",
                new BigDecimal("500.00"), new BigDecimal("400.00"));
        when(pagoRepository.findByIdempotencyKey("clave-3")).thenReturn(Optional.of(previo));

        PagoResponse respuesta = pagoService.autorizar("clave-3", request, metadatos);

        assertThat(respuesta.replicado()).isTrue();
        assertThat(respuesta.estado()).isEqualTo(EstadoPago.AUTORIZADO);
        verify(cuentaRepository, never()).save(any(Cuenta.class));
        verify(pagoRepository, never()).saveAndFlush(any());
    }
}


    @Test
    void limiteExcedidoNoCobra() {
        PagoRequest request = new PagoRequest("S-1001", new BigDecimal("1500.00"), "REF-LIM");
        when(pagoRepository.findByIdempotencyKey("clave-lim")).thenReturn(Optional.empty());
        when(cuentaRepository.findById("S-1001"))
                .thenReturn(Optional.of(new Cuenta("S-1001", new BigDecimal("5000.00"), new BigDecimal("1000.00"),
                        new BigDecimal("5000.00"), BigDecimal.ZERO)));
        PagoResponse respuesta = pagoService.autorizar("clave-lim", request, metadatos);
        assertThat(respuesta.estado()).isEqualTo(EstadoPago.RECHAZADO);
        assertThat(respuesta.motivoRechazo()).contains("Límite");
        verify(cuentaRepository, never()).save(any(Cuenta.class));
    }

    @Test
    void bancoNoRespondeQuedaPendiente() {
        PagoRequest request = new PagoRequest("S-1001", new BigDecimal("100.00"), "REF-PEND");
        when(pagoRepository.findByIdempotencyKey("clave-pend")).thenReturn(Optional.empty());
        when(cuentaRepository.findById("S-1001"))
                .thenReturn(Optional.of(new Cuenta("S-1001", new BigDecimal("5000.00"), new BigDecimal("1000.00"),
                        new BigDecimal("5000.00"), BigDecimal.ZERO)));
        CompletableFuture<BancoRespuesta> future = new CompletableFuture<>();
        when(servicioBanco.autorizarPago(anyString(), any(), any())).thenReturn(future);
        PagoResponse respuesta = pagoService.autorizar("clave-pend", request, metadatos);
        assertThat(respuesta.estado()).isEqualTo(EstadoPago.PENDIENTE_BANCO);
    }
