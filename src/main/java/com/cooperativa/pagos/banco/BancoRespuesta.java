package com.cooperativa.pagos.banco;

public record BancoRespuesta(boolean autorizado, String motivo, boolean error) {
}

