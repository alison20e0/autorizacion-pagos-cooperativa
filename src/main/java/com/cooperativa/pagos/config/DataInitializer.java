package com.cooperativa.pagos.config;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cooperativa.pagos.domain.Cuenta;
import com.cooperativa.pagos.repository.CuentaRepository;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initCuentas(CuentaRepository cuentaRepository) {
        return args -> {
            if (cuentaRepository.count() == 0) {
                cuentaRepository.save(new Cuenta("S-1001", new BigDecimal("1500.00"), new BigDecimal("1000.00"),
                        new BigDecimal("5000.00"), BigDecimal.ZERO));
                cuentaRepository.save(new Cuenta("S-1002", new BigDecimal("250.00"), new BigDecimal("1000.00"),
                        new BigDecimal("5000.00"), BigDecimal.ZERO));
                cuentaRepository.save(new Cuenta("S-1003", new BigDecimal("80.00"), new BigDecimal("1000.00"),
                        new BigDecimal("5000.00"), BigDecimal.ZERO));
            }
        };
    }
}

