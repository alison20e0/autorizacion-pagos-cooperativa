package com.cooperativa.pagos.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificacionSocioConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificacionSocioConsumer.class);

    @RabbitListener(queues = "${app.rabbitmq.queue:pago.procesado}")
    public void recibir(PagoProcesadoEvent event) {
        log.info("[NOTIFICACION AL SOCIO] Recibido evento para pagoId={}, socio={}, estado={}, monto={}",
                event.pagoId(), event.numeroSocio(), event.estado(), event.monto());
        if (event.motivoRechazo() != null) {
            log.info("[NOTIFICACION AL SOCIO] Motivo: {}", event.motivoRechazo());
        }
    }
}

