package com.cooperativa.pagos.exception;

public class LimiteExcedidoException extends RuntimeException {

    public LimiteExcedidoException(String mensaje) {
        super(mensaje);
    }

    public LimiteExcedidoException(String mensaje, String detalle) {
        super(mensaje + ": " + detalle);
    }
}