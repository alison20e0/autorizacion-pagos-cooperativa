package com.cooperativa.pagos.exception;

public class LimiteExcedidoException extends RuntimeException {
    public LimiteExcedidoException(String tipo, String detalle) {
        super("LÃ­mite excedido: " + tipo + " - " + detalle);
    }
}
