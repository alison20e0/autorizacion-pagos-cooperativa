package com.cooperativa.pagos.domain;

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
@Table(name = "auditoria")
public class Auditoria {

    public enum Evento {
        SOLICITUD_RECIBIDA,
        PAGO_AUTORIZADO,
        PAGO_RECHAZADO,
        CONFLICTO_IDEMPOTENCIA
    }

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "pago_id")
    private UUID pagoId;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "evento", nullable = false, length = 32)
    private Evento evento;

    @Column(name = "detalle", length = 512)
    private String detalle;

    @Column(name = "usuario", length = 64)
    private String usuario;

    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;

    @Column(name = "fecha", nullable = false)
    private Instant fecha;

    @PrePersist
    void asignarValoresPorDefecto() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.fecha == null) {
            this.fecha = Instant.now();
        }
    }

    public static Auditoria registrar(Evento evento, String idempotencyKey, UUID pagoId,
                                      String detalle, String usuario, String direccionIp) {
        Auditoria auditoria = new Auditoria();
        auditoria.evento = evento;
        auditoria.idempotencyKey = idempotencyKey;
        auditoria.pagoId = pagoId;
        auditoria.detalle = detalle;
        auditoria.usuario = usuario;
        auditoria.direccionIp = direccionIp;
        return auditoria;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPagoId() {
        return pagoId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Evento getEvento() {
        return evento;
    }

    public String getDetalle() {
        return detalle;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getDireccionIp() {
        return direccionIp;
    }

    public Instant getFecha() {
        return fecha;
    }
}