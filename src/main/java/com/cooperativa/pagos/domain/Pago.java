package com.cooperativa.pagos.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "numero_socio", nullable = false, length = 32)
    private String numeroSocio;

    @Column(name = "monto", nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(name = "referencia", length = 64)
    private String referencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 16)
    private EstadoPago estado;

    @Column(name = "motivo_rechazo", length = 255)
    private String motivoRechazo;

    @Column(name = "saldo_antes", precision = 19, scale = 2)
    private BigDecimal saldoAntes;

    @Column(name = "saldo_despues", precision = 19, scale = 2)
    private BigDecimal saldoDespues;

    @Column(name = "fecha_proceso", nullable = false)
    private Instant fechaProceso;

    @PrePersist
    void asignarValoresPorDefecto() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.fechaProceso == null) {
            this.fechaProceso = Instant.now();
        }
    }

    public static Pago autorizar(String idempotencyKey, String numeroSocio, BigDecimal monto,
                                 String referencia, BigDecimal saldoAntes, BigDecimal saldoDespues) {
        Pago pago = new Pago();
        pago.idempotencyKey = idempotencyKey;
        pago.numeroSocio = numeroSocio;
        pago.monto = monto;
        pago.referencia = referencia;
        pago.estado = EstadoPago.AUTORIZADO;
        pago.saldoAntes = saldoAntes;
        pago.saldoDespues = saldoDespues;
        return pago;
    }

    public static Pago rechazar(String idempotencyKey, String numeroSocio, BigDecimal monto,
                                 String referencia, String motivoRechazo, BigDecimal saldoAntes) {
        Pago pago = new Pago();
        pago.idempotencyKey = idempotencyKey;
        pago.numeroSocio = numeroSocio;
        pago.monto = monto;
        pago.referencia = referencia;
        pago.estado = EstadoPago.RECHAZADO;
        pago.motivoRechazo = motivoRechazo;
        pago.saldoAntes = saldoAntes;
        pago.saldoDespues = saldoAntes;
        return pago;
    }

    public void registrarSaldoDespues(BigDecimal saldoDespues) {
        this.saldoDespues = saldoDespues;
    }

    public boolean esAutorizado() {
        return this.estado == EstadoPago.AUTORIZADO;
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getNumeroSocio() {
        return numeroSocio;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getReferencia() {
        return referencia;
    }

    public EstadoPago getEstado() {
        return estado;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public BigDecimal getSaldoAntes() {
        return saldoAntes;
    }

    public BigDecimal getSaldoDespues() {
        return saldoDespues;
    }

    public Instant getFechaProceso() {
        return fechaProceso;
    }
}