package com.cooperativa.pagos.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cooperativa.pagos.domain.Pago;

@Component
public class PagoProcesadoPublisher {

    private static final Logger log = LoggerFactory.getLogger(PagoProcesadoPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange:pago.events}")
    private String exchangeName;

    @Value("${app.rabbitmq.routing-key:pago.procesado}")
    private String routingKey;

    public PagoProcesadoPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicar(Pago pago) {
        PagoProcesadoEvent event = new PagoProcesadoEvent(
                pago.getId(),
                pago.getIdempotencyKey(),
                pago.getNumeroSocio(),
                pago.getMonto(),
                pago.getEstado() != null ? pago.getEstado().name() : null,
                pago.getMotivoRechazo(),
                pago.getFechaProceso()
        );
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
            log.info("Evento PagoProcesado publicado: pagoId={}, estado={}", pago.getId(), pago.getEstado());
        } catch (Exception e) {
            log.error("Error al publicar evento PagoProcesado para pagoId={}", pago.getId(), e);
        }
    }
}

