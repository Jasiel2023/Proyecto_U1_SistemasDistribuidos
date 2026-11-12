package ec.unl.votacion.votacion_service.application;

import ec.unl.votacion.votacion_service.api.EmitirVotoRequest;
import ec.unl.votacion.votacion_service.api.VotoResponse;
import ec.unl.votacion.votacion_service.messaging.VotoEmitidoEvent;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class VotacionService {

    private static final Logger log = LoggerFactory.getLogger(VotacionService.class);

    private final JdbcTemplate jdbcTemplate;
    private final RabbitTemplate rabbitTemplate;

    public VotacionService(JdbcTemplate jdbcTemplate, RabbitTemplate rabbitTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public VotoResponse emitir(EmitirVotoRequest request) {
        Boolean abierta = jdbcTemplate.query("SELECT esta_activa FROM elecciones WHERE id = ?",
                rs -> rs.next() ? rs.getBoolean(1) : null, request.eleccionId());
        if (abierta == null) {
            throw new EleccionNoEncontradaException("No existe la eleccion " + request.eleccionId());
        }
        if (!abierta) {
            throw new VotoInvalidoException("La eleccion no esta abierta");
        }
        Integer candidatoValido = jdbcTemplate.query("SELECT 1 FROM candidatos WHERE id = ? AND eleccion_id = ? AND activo = TRUE",
                rs -> rs.next() ? 1 : null, request.candidatoId(), request.eleccionId());
        if (candidatoValido == null) {
            throw new VotoInvalidoException("El candidato no pertenece a la eleccion");
        }

        UUID votoId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        OffsetDateTime ocurrido = OffsetDateTime.now(ZoneOffset.UTC);
        try {
            UUID usuarioId = UUID.nameUUIDFromBytes(("cuenta:" + request.votanteId()).getBytes(StandardCharsets.UTF_8));
            jdbcTemplate.update("INSERT INTO votos (id, eleccion_id, candidato_id, id_usuario, hora, emitido_en, votante_id) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    votoId, request.eleccionId(), request.candidatoId(), usuarioId,
                    ocurrido.toLocalTime(), ocurrido, request.votanteId());
        } catch (DuplicateKeyException ex) {
            throw new VotoDuplicadoException("VOTO_DUPLICADO: el votante ya emitio un voto en esta eleccion");
        }

        VotoEmitidoEvent event = new VotoEmitidoEvent(eventId, 1, votoId,
                request.eleccionId(), request.candidatoId(), ocurrido);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rabbitTemplate.convertAndSend("votacion.exchange", "voto.emitido", event);
                log.info("Evento voto.emitido publicado eventId={} votoId={} eleccionId={} candidatoId={}",
                        event.eventId(), event.votoId(), event.eleccionId(), event.candidatoId());
            }
        });
        return new VotoResponse(true, "VOTO_ACEPTADO", votoId);
    }
}
