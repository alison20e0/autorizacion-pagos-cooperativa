package com.cooperativa.pagos.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PagoRequest(

        @NotBlank(message = "numeroSocio es obligatorio")
        @Size(max = 32, message = "numeroSocio no debe exceder 32 caracteres")
        String numeroSocio,

        @NotNull(message = "monto es obligatorio")
        @DecimalMin(value = "0.01", message = "monto debe ser mayor a cero")
        @Digits(integer = 17, fraction = 2, message = "monto admite hasta 2 decimales")
        BigDecimal monto,

        @Size(max = 64, message = "referencia no debe exceder 64 caracteres")
        String referencia) {
}