package com.cooperativa.pagos.exception;

public class ServicioBancoNoDisponibleException extends RuntimeException {
    public ServicioBancoNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}

