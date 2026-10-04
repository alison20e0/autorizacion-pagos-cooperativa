package com.cooperativa.pagos.saldo;

import java.math.BigDecimal;

/**
 * Contrato de consulta de saldo del socio. La implementacion actual es una
 * simulacion en memoria; puede reemplazarse por el cliente del core bancario
 * sin modificar el controlador ni el servicio de pagos.
 */
public interface ServicioSaldo {

    BigDecimal consultarSaldo(String numeroSocio);

    void descontar(String numeroSocio, BigDecimal monto);
}