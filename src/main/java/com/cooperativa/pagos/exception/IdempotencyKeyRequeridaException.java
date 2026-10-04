package com.cooperativa.pagos.exception;

public class IdempotencyKeyRequeridaException extends RuntimeException {

    public IdempotencyKeyRequeridaException() {
        super("El header X-Idempotency-Key es obligatorio para autorizar pagos");
    }
}