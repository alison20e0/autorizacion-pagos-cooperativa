package com.cooperativa.pagos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cooperativa.pagos.domain.Cuenta;

public interface CuentaRepository extends JpaRepository<Cuenta, String> {
}

