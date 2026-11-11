package ec.unl.votacion.resultados.application;

import ec.unl.votacion.resultados.domain.EventoConflicto;
import ec.unl.votacion.resultados.domain.VotoProcesado;
import ec.unl.votacion.resultados.infrastructure.persistence.EventoConflictoRepository;
import ec.unl.votacion.resultados.infrastructure.persistence.ProyeccionRepository;
import ec.unl.votacion.resultados.infrastructure.persistence.VotoProcesadoRepository;
import ec.unl.votacion.resultados.messaging.VotoEmitidoEvent;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResultadoCommandService {

    private static final Logger log = LoggerFactory.getLogger(ResultadoCommandService.class);

    private final ProyeccionRepository proyeccionRepository;
    private final VotoProcesadoRepository votoRepository;
    private final EventoConflictoRepository conflictoRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Validator validator;

    public ResultadoCommandService(ProyeccionRepository proyeccionRepository,
                                   VotoProcesadoRepository votoRepository,
                                   EventoConflictoRepository conflictoRepository,
                                   ApplicationEventPublisher eventPublisher,
                                   Validator validator) {
        this.proyeccionRepository = proyeccionRepository;
        this.votoRepository = votoRepository;
        this.conflictoRepository = conflictoRepository;
        this.eventPublisher = eventPublisher;
        this.validator = validator;
    }

    @Transactional
    public ProcesamientoResultado procesar(VotoEmitidoEvent evento) {
        validar(evento);

        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        VotoProcesado voto = new VotoProcesado(
                evento.eventId(), evento.votoId(), evento.eleccionId(),
                evento.candidatoId(), evento.occurredAt(), ahora);

        if (!proyeccionRepository.insertarVoto(voto)) {
            manejarDuplicado(evento);
            return new ProcesamientoResultado(false, -1L, -1L);
        }

        RevisionActualizada revision = proyeccionRepository.incrementarResumen(
                evento.eleccionId(), ahora);
        proyeccionRepository.incrementarConteo(evento.eleccionId(), evento.candidatoId(), ahora);

        eventPublisher.publishEvent(new ResultadoActualizadoEvent(evento.eleccionId()));
        return new ProcesamientoResultado(true, revision.revision(), revision.totalVotos());
    }

    private void validar(VotoEmitidoEvent evento) {
        if (evento == null) {
            throw new EventoInvalidoException("El evento es nulo");
        }
        Set<ConstraintViolation<VotoEmitidoEvent>> violaciones = validator.validate(evento);
        if (!violaciones.isEmpty()) {
            throw new EventoInvalidoException("Evento invalido: " + violaciones.iterator().next().getMessage());
        }
    }

    private void manejarDuplicado(VotoEmitidoEvent evento) {
        Optional<VotoProcesado> porEvento = votoRepository.findById(evento.eventId());
        if (porEvento.isPresent()) {
            VotoProcesado existente = porEvento.get();
            if (!mismoVoto(existente, evento)) {
                registrarConflicto(evento,
                        "EVENT_ID_PAYLOAD_INCOMPATIBLE",
                        "El eventId ya existe con eleccionId, votoId o candidatoId distintos");
            } else {
                log.info("Evento duplicado ignorado eventId={}", evento.eventId());
            }
            return;
        }

        Optional<VotoProcesado> porVotoLogico = votoRepository.findByEleccionIdAndVotoId(
                evento.eleccionId(), evento.votoId());
        if (porVotoLogico.isPresent()) {
            VotoProcesado existente = porVotoLogico.get();
            if (!existente.getCandidatoId().equals(evento.candidatoId())) {
                registrarConflicto(evento,
                        "VOTO_LOGICO_PAYLOAD_INCOMPATIBLE",
                        "El voto logico ya existe con candidatoId distinto");
            } else {
                log.info("Voto logico duplicado ignorado eleccionId={} votoId={} eventId={}",
                        evento.eleccionId(), evento.votoId(), evento.eventId());
            }
            return;
        }

        log.warn("Inconsistencia de deduplicacion eventId={} eleccionId={} votoId={}",
                evento.eventId(), evento.eleccionId(), evento.votoId());
    }

    private boolean mismoVoto(VotoProcesado existente, VotoEmitidoEvent evento) {
        return existente.getEleccionId().equals(evento.eleccionId())
                && existente.getVotoId().equals(evento.votoId())
                && existente.getCandidatoId().equals(evento.candidatoId());
    }

    private void registrarConflicto(VotoEmitidoEvent evento, String motivo, String detalle) {
        log.warn("Conflicto de evento eventId={} motivo={}", evento.eventId(), motivo);
        conflictoRepository.save(new EventoConflicto(
                evento.eventId(), motivo, detalle, OffsetDateTime.now(ZoneOffset.UTC)));
    }
}
