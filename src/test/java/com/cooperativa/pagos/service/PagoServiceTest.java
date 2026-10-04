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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cooperativa.pagos.domain.EstadoPago;
import com.cooperativa.pagos.domain.Pago;
import com.cooperativa.pagos.dto.PagoRequest;
import com.cooperativa.pagos.dto.PagoResponse;
import com.cooperativa.pagos.exception.IdempotencyKeyRequeridaException;
import com.cooperativa.pagos.repository.AuditoriaRepository;
import com.cooperativa.pagos.repository.PagoRepository;
import com.cooperativa.pagos.saldo.ServicioSaldo;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private AuditoriaRepository auditoriaRepository;

    @Mock
    private ServicioSaldo servicioSaldo;

    private PagoService pagoService;
    private MetadatosRequest metadatos;

    @BeforeEach
    void setUp() {
        pagoService = new PagoService(pagoRepository, auditoriaRepository, servicioSaldo);
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
        when(servicioSaldo.consultarSaldo("S-1001")).thenReturn(new BigDecimal("500.00"));

        PagoResponse respuesta = pagoService.autorizar("clave-1", request, metadatos);

        assertThat(respuesta.estado()).isEqualTo(EstadoPago.AUTORIZADO);
        assertThat(respuesta.replicado()).isFalse();
        assertThat(respuesta.saldoDespues()).isEqualByComparingTo("400.00");
        verify(servicioSaldo, times(1)).descontar("S-1001", new BigDecimal("100.00"));
    }

    @Test
    void rechazaSinDescontarCuandoElSaldoNoAlcanza() {
        PagoRequest request = new PagoRequest("S-1003", new BigDecimal("500.00"), "REF-3");
        when(pagoRepository.findByIdempotencyKey("clave-2")).thenReturn(Optional.empty());
        when(servicioSaldo.consultarSaldo("S-1003")).thenReturn(new BigDecimal("80.00"));

        PagoResponse respuesta = pagoService.autorizar("clave-2", request, metadatos);

        assertThat(respuesta.estado()).isEqualTo(EstadoPago.RECHAZADO);
        assertThat(respuesta.motivoRechazo()).contains("Saldo insuficiente");
        verify(servicioSaldo, never()).descontar(anyString(), any());
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
        verify(servicioSaldo, never()).descontar(anyString(), any());
        verify(pagoRepository, never()).saveAndFlush(any());
    }
}