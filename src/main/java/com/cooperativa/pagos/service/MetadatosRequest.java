package com.cooperativa.pagos.service;

/**
 * Datos de contexte enviados por el gateway o capa de seguridad.
 */
public record MetadatosRequest(String usuario, String direccionIp) {

    public static MetadatosRequest porDefecto() {
        return new MetadatosRequest("desconocido", null);
    }
}