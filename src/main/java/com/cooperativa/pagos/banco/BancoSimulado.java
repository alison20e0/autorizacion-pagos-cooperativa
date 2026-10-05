package com.cooperativa.pagos.banco;

import java.math.BigDecimal;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BancoSimulado implements ServicioBanco {

    private static final Logger log = LoggerFactory.getLogger(BancoSimulado.class);
    private final Executor executor = Executors.newCachedThreadPool();
    private final Random random = new Random();

    @Value("${banco.simular.demora-ms:0}")
    private long demoraMs;

    @Value("${banco.simular.fallo:false}")
    private boolean simularFallo;

    @Override
    public CompletableFuture<BancoRespuesta> autorizarPago(String numeroSocio, BigDecimal monto, String referencia) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (demoraMs > 0) {
                    log.info("Simulando demora del banco: {} ms", demoraMs);
                    TimeUnit.MILLISECONDS.sleep(demoraMs);
                }
                if (simularFallo) {
                    log.warn("Simulando fallo del banco para socio {}", numeroSocio);
                    return new BancoRespuesta(false, "Fallo simulado en servicio bancario", true);
                }
                if (random.nextDouble() < 0.05) {
                    return new BancoRespuesta(false, "Rechazo por politica interna del banco", false);
                }
                return new BancoRespuesta(true, "Autorizado por banco", false);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new BancoRespuesta(false, "Servicio bancario interrumpido", true);
            }
        }, executor);
    }
}

