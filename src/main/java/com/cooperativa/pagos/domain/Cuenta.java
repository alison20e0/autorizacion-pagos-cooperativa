package com.cooperativa.pagos.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cuenta")
public class Cuenta {

    @Id
    @Column(name = "numero_socio", nullable = false, length = 32)
    private String numeroSocio;

    @Column(name = "saldo", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldo;

    @Column(name = "limite_pago", precision = 19, scale = 2)
    private BigDecimal limitePago;

    @Column(name = "limite_diario", precision = 19, scale = 2)
    private BigDecimal limiteDiario;

    @Column(name = "gasto_diario", precision = 19, scale = 2)
    private BigDecimal gastoDiario;

    public Cuenta() {
    }

    public Cuenta(String numeroSocio, BigDecimal saldo, BigDecimal limitePago, BigDecimal limiteDiario,
                  BigDecimal gastoDiario) {
        this.numeroSocio = numeroSocio;
        this.saldo = saldo;
        this.limitePago = limitePago;
        this.limiteDiario = limiteDiario;
        this.gastoDiario = gastoDiario;
    }

    public String getNumeroSocio() {
        return numeroSocio;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public BigDecimal getLimitePago() {
        return limitePago;
    }

    public BigDecimal getLimiteDiario() {
        return limiteDiario;
    }

    public BigDecimal getGastoDiario() {
        return gastoDiario;
    }

    public void debitar(BigDecimal monto) {
        this.saldo = this.saldo.subtract(monto);
        if (this.gastoDiario == null) {
            this.gastoDiario = BigDecimal.ZERO;
        }
        this.gastoDiario = this.gastoDiario.add(monto);
    }

    public void actualizarGastoDiario(BigDecimal monto) {
        this.gastoDiario = monto;
    }
}

