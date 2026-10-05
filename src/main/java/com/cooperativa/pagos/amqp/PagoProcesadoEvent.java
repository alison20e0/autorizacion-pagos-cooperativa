package com.cooperativa.pagos.amqp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PagoProcesadoEvent(
        UUID pagoId,
        String idempotencyKey,
        String numeroSocio,
        BigDecimal monto,
        String estado,
        String motivoRechazo,
        Instant fechaProceso
) implements Serializable {
}

