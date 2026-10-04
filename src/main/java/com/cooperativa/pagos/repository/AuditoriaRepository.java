package com.cooperativa.pagos.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cooperativa.pagos.domain.Auditoria;

public interface AuditoriaRepository extends JpaRepository<Auditoria, UUID> {

    java.util.List<Auditoria> findByPagoIdOrderByFechaAsc(UUID pagoId);
}