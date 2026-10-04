package com.cooperativa.pagos.exception;

import java.math.BigDecimal;

public class SaldoInsuficienteException extends RuntimeException {

    private final String numeroSocio;
    private final BigDecimal saldoDisponible;
    private final BigDecimal montoSolicitado;

    public SaldoInsuficienteException(String numeroSocio, BigDecimal saldoDisponible, BigDecimal montoSolicitado) {
        super("Saldo insuficiente para el socio " + numeroSocio
                + ": disponible " + saldoDisponible + ", solicitado " + montoSolicitado);
        this.numeroSocio = numeroSocio;
        this.saldoDisponible = saldoDisponible;
        this.montoSolicitado = montoSolicitado;
    }

    public String getNumeroSocio() {
        return numeroSocio;
    }

    public BigDecimal getSaldoDisponible() {
        return saldoDisponible;
    }

    public BigDecimal getMontoSolicitado() {
        return montoSolicitado;
    }
}