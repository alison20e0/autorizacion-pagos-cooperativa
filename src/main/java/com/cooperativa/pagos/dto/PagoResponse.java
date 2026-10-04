package com.cooperativa.pagos.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.cooperativa.pagos.domain.EstadoPago;
import com.cooperativa.pagos.domain.Pago;

public record PagoResponse(
        UUID id,
        String idempotencyKey,
        String numeroSocio,
        BigDecimal monto,
        String referencia,
        EstadoPago estado,
        String motivoRechazo,
        BigDecimal saldoAntes,
        BigDecimal saldoDespues,
        Instant fechaProceso,
        boolean replicado) {

    public static PagoResponse desde(Pago pago, boolean replicado) {
        return new PagoResponse(
                pago.getId(),
                pago.getIdempotencyKey(),
                pago.getNumeroSocio(),
                pago.getMonto(),
                pago.getReferencia(),
                pago.getEstado(),
                pago.getMotivoRechazo(),
                pago.getSaldoAntes(),
                pago.getSaldoDespues(),
                pago.getFechaProceso(),
                replicado);
    }
}