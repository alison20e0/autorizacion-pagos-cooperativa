package com.cooperativa.pagos.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cooperativa.pagos.domain.EstadoPago;
import com.cooperativa.pagos.domain.Pago;

public interface PagoRepository extends JpaRepository<Pago, UUID> {

    Optional<Pago> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<Pago> findByEstado(EstadoPago estado);
}

