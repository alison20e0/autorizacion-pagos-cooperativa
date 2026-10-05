package com.cooperativa.pagos.banco;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

public interface ServicioBanco {
    CompletableFuture<BancoRespuesta> autorizarPago(String numeroSocio, BigDecimal monto, String referencia);
}

