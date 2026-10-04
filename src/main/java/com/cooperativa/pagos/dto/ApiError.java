package com.cooperativa.pagos.dto;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String mensaje,
        String path) {
}