package com.cooperativa.pagos.saldo;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cooperativa.pagos.exception.SaldoInsuficienteException;

/**
 * Simulacion del saldo en cuenta del socio con deduccion atomica del monto.
 * Los saldos viven en memoria: al reiniciar el proceso vuelven a su valor inicial.
 */
@Service
public class ServicioSaldoSimulado implements ServicioSaldo {

    private static final Logger log = LoggerFactory.getLogger(ServicioSaldoSimulado.class);

    private static final Map<String, BigDecimal> SALDOS = new ConcurrentHashMap<>(Map.of(
            "S-1001", new BigDecimal("1500.00"),
            "S-1002", new BigDecimal("250.00"),
            "S-1003", new BigDecimal("80.00")));

    private static final BigDecimal SALDO_POR_DEFECTO = new BigDecimal("0.00");

    @Override
    public BigDecimal consultarSaldo(String numeroSocio) {
        return SALDOS.getOrDefault(numeroSocio, SALDO_POR_DEFECTO);
    }

    @Override
    public void descontar(String numeroSocio, BigDecimal monto) {
        BigDecimal saldoAntes = consultarSaldo(numeroSocio);

        if (saldoAntes.compareTo(monto) < 0) {
            log.warn("Rechazo por saldo insuficiente: socio={} saldo={} monto={}",
                    numeroSocio, saldoAntes, monto);
            throw new SaldoInsuficienteException(numeroSocio, saldoAntes, monto);
        }

        BigDecimal saldoDespues = saldoAntes.subtract(monto);
        SALDOS.put(numeroSocio, saldoDespues);
        log.info("Saldo actualizado: socio={} antes={} despues={}", numeroSocio, saldoAntes, saldoDespues);
    }
}