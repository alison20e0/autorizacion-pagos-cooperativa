package com.cooperativa.pagos.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "pagos.limites")
public class LimitesConfig {
    private BigDecimal limitePago = new BigDecimal("1000.00");
    private BigDecimal limiteDiario = new BigDecimal("5000.00");

    public BigDecimal getLimitePago() {
        return limitePago;
    }

    public void setLimitePago(BigDecimal limitePago) {
        this.limitePago = limitePago;
    }

    public BigDecimal getLimiteDiario() {
        return limiteDiario;
    }

    public void setLimiteDiario(BigDecimal limiteDiario) {
        this.limiteDiario = limiteDiario;
    }
}

