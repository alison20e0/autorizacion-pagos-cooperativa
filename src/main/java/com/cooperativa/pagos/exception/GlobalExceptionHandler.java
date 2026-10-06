package com.cooperativa.pagos.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.cooperativa.pagos.dto.ApiError;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IdempotencyKeyRequeridaException.class)
    public ResponseEntity<ApiError> sinIdempotencyKey(IdempotencyKeyRequeridaException ex,
                                                       HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(SaldoInsuficienteException.class)
    public ResponseEntity<ApiError> saldoInsuficiente(SaldoInsuficienteException ex,
                                                       HttpServletRequest request) {
        return construir(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacionFallida(MethodArgumentNotValidException ex,
                                                       HttpServletRequest request) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return construir(HttpStatus.BAD_REQUEST, mensaje, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> argumentoInvalido(IllegalArgumentException ex,
                                                      HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(LimiteExcedidoException.class)
    public ResponseEntity<ApiError> limiteExcedido(LimiteExcedidoException ex,
                                                    HttpServletRequest request) {
        return construir(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    @ExceptionHandler(PagoNoEncontradoException.class)
    public ResponseEntity<ApiError> pagoNoEncontrado(PagoNoEncontradoException ex,
                                                     HttpServletRequest request) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(ServicioBancoNoDisponibleException.class)
    public ResponseEntity<ApiError> bancoNoDisponible(ServicioBancoNoDisponibleException ex,
                                                       HttpServletRequest request) {
        return construir(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> errorInesperado(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado procesando {}", request.getRequestURI(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno al procesar el pago", request);
    }

    private ResponseEntity<ApiError> construir(HttpStatus status, String mensaje,
                                               HttpServletRequest request) {
        ApiError error = new ApiError();
        error.setTimestamp(Instant.now());
        error.setStatus(status.value());
        error.setError(status.getReasonPhrase());
        error.setMessage(mensaje);
        error.setPath(request.getRequestURI());
        return ResponseEntity.status(status).body(error);
    }
}
