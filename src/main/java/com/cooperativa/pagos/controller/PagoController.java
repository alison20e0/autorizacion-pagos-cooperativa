package com.cooperativa.pagos.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cooperativa.pagos.dto.PagoRequest;
import com.cooperativa.pagos.dto.PagoResponse;
import com.cooperativa.pagos.service.MetadatosRequest;
import com.cooperativa.pagos.service.PagoService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/pagos")
public class PagoController {

    private static final Logger log = LoggerFactory.getLogger(PagoController.class);
    private static final String HEADER_IDEMPOTENCY = "X-Idempotency-Key";
    private static final String HEADER_REPLAY = "Idempotency-Replayed";

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping
    public ResponseEntity<PagoResponse> registrarPago(
            @RequestHeader(name = HEADER_IDEMPOTENCY, required = false) String idempotencyKey,
            @Valid @RequestBody PagoRequest request,
            HttpServletRequest servletRequest) {

        MetadatosRequest metadatos = new MetadatosRequest(
                resolverUsuario(servletRequest), servletRequest.getRemoteAddr());

        PagoResponse respuesta = pagoService.autorizar(idempotencyKey, request, metadatos);

        HttpStatus status = switch (respuesta.estado()) {
            case AUTORIZADO -> respuesta.replicado() ? HttpStatus.OK : HttpStatus.CREATED;
            case RECHAZADO -> HttpStatus.UNPROCESSABLE_ENTITY;
            case PENDIENTE_BANCO -> HttpStatus.ACCEPTED;
            case CONCILIADO -> HttpStatus.OK;
        };

        log.info("POST /api/v1/pagos clave={} socio={} estado={} replicado={}",
                idempotencyKey, request.numeroSocio(), respuesta.estado(), respuesta.replicado());

        return ResponseEntity.status(status)
                .header(HEADER_REPLAY, Boolean.toString(respuesta.replicado()))
                .body(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagoResponse> obtenerPago(@PathVariable String id, HttpServletRequest servletRequest) {
        PagoResponse respuesta = pagoService.obtenerPorId(id);
        return ResponseEntity.ok(respuesta);
    }

    private String resolverUsuario(HttpServletRequest request) {
        String usuario = request.getHeader("X-Usuario");
        return usuario != null && !usuario.isBlank() ? usuario : "desconocido";
    }
}

