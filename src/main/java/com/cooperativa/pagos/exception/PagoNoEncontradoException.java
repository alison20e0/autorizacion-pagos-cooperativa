package com.cooperativa.pagos.exception;

public class PagoNoEncontradoException extends RuntimeException {
    public PagoNoEncontradoException() {
        super("Pago no encontrado");
    }
}

