package com.cooperativa.pagos.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cooperativa.pagos.domain.Auditoria;
import com.cooperativa.pagos.domain.EstadoPago;
import com.cooperativa.pagos.domain.Pago;
import com.cooperativa.pagos.repository.AuditoriaRepository;
import com.cooperativa.pagos.repository.PagoRepository;

@Service
public class ConciliacionService {

    private static final Logger log = LoggerFactory.getLogger(ConciliacionService.class);

    private final PagoRepository pagoRepository;
    private final AuditoriaRepository auditoriaRepository;

    public ConciliacionService(PagoRepository pagoRepository, AuditoriaRepository auditoriaRepository) {
        this.pagoRepository = pagoRepository;
        this.auditoriaRepository = auditoriaRepository;
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void conciliar() {
        List<Pago> pendientes = pagoRepository.findByEstado(EstadoPago.PENDIENTE_BANCO);
        if (pendientes.isEmpty()) {
            return;
        }
        log.info("Iniciando conciliacion de {} pagos PENDIENTE_BANCO", pendientes.size());
        for (Pago pago : pendientes) {
            // Simulamos conciliacion: marca como CONCILIADO por defecto
            pago.cambiarEstado(EstadoPago.CONCILIADO);
            pagoRepository.save(pago);
            auditoriaRepository.save(Auditoria.registrar(
                    Auditoria.Evento.CONCILIACION_FINALIZADA,
                    pago.getIdempotencyKey(),
                    pago.getId(),
                    "Conciliado desde PENDIENTE_BANCO",
                    "sistema",
                    "127.0.0.1"));
            log.info("Pago {} conciliado", pago.getId());
        }
    }
}

