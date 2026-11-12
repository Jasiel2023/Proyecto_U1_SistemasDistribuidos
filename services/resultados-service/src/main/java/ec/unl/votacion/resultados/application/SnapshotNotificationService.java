package ec.unl.votacion.resultados.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class SnapshotNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SnapshotNotificationService.class);

    private final ResultadoQueryService queryService;
    private final SimpMessagingTemplate messagingTemplate;
    private final String topicPrefix;

    public SnapshotNotificationService(ResultadoQueryService queryService,
                                       SimpMessagingTemplate messagingTemplate,
                                       ec.unl.votacion.resultados.config.ResultadosProperties properties) {
        this.queryService = queryService;
        this.messagingTemplate = messagingTemplate;
        this.topicPrefix = properties.websocket().topicPrefix();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notificar(ResultadoActualizadoEvent evento) {
        try {
            ResultadoSnapshot snapshot = queryService.obtener(evento.eleccionId());
            log.info("Difundiendo snapshot eleccionId={} revision={} a {}",
                    snapshot.eleccionId(), snapshot.revision(), destino(evento));
            messagingTemplate.convertAndSend(destino(evento), snapshot);
        } catch (RuntimeException ex) {
            log.error("No se pudo difundir el snapshot de eleccionId={}", evento.eleccionId(), ex);
        }
    }

    private String destino(ResultadoActualizadoEvent evento) {
        String prefijo = topicPrefix.endsWith("/") ? topicPrefix.substring(0, topicPrefix.length() - 1) : topicPrefix;
        return prefijo + "/" + evento.eleccionId();
    }
}
