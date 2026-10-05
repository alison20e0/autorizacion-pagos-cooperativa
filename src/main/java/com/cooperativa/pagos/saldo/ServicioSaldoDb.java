package com.cooperativa.pagos.saldo;

import java.math.BigDecimal;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cooperativa.pagos.domain.Cuenta;
import com.cooperativa.pagos.exception.SaldoInsuficienteException;
import com.cooperativa.pagos.repository.CuentaRepository;

@Service
@Primary
public class ServicioSaldoDb implements ServicioSaldo {

    private final CuentaRepository cuentaRepository;

    public ServicioSaldoDb(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal consultarSaldo(String numeroSocio) {
        return cuentaRepository.findById(numeroSocio)
                .map(Cuenta::getSaldo)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public void descontar(String numeroSocio, BigDecimal monto) {
        Cuenta cuenta = cuentaRepository.findById(numeroSocio)
                .orElseThrow(() -> new SaldoInsuficienteException(numeroSocio, BigDecimal.ZERO, monto));
        if (cuenta.getSaldo().compareTo(monto) < 0) {
            throw new SaldoInsuficienteException(numeroSocio, cuenta.getSaldo(), monto);
        }
        cuenta.debitar(monto);
        cuentaRepository.save(cuenta);
    }
}

