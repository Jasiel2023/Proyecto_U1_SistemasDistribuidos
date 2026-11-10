package ec.unl.votacion.votacion_service.service;

import ec.unl.votacion.votacion_service.dto.CrearVotoRequest;
import ec.unl.votacion.votacion_service.dto.VotoResponse;
import ec.unl.votacion.votacion_service.entity.Opcion;
import ec.unl.votacion.votacion_service.entity.Votacion;
import ec.unl.votacion.votacion_service.entity.Voto;
import ec.unl.votacion.votacion_service.exception.OpcionInactivaException;
import ec.unl.votacion.votacion_service.exception.OpcionNoEncontradaException;
import ec.unl.votacion.votacion_service.exception.VotacionCerradaException;
import ec.unl.votacion.votacion_service.exception.VotacionNoEncontradaException;
import ec.unl.votacion.votacion_service.exception.VotoDuplicadoException;
import ec.unl.votacion.votacion_service.messaging.VotoEmitidoEvent;
import ec.unl.votacion.votacion_service.repository.OpcionRepository;
import ec.unl.votacion.votacion_service.repository.VotacionRepository;
import ec.unl.votacion.votacion_service.repository.VotoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class VotoService {

    private final VotacionRepository votacionRepository;
    private final OpcionRepository opcionRepository;
    private final VotoRepository votoRepository;
    private final ApplicationEventPublisher eventPublisher;

    public VotoService(
            VotacionRepository votacionRepository,
            OpcionRepository opcionRepository,
            VotoRepository votoRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.votacionRepository = votacionRepository;
        this.opcionRepository = opcionRepository;
        this.votoRepository = votoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public VotoResponse registrarVoto(CrearVotoRequest request) {
        Votacion votacion = votacionRepository.findById(request.votacionId())
                .orElseThrow(() -> new VotacionNoEncontradaException(request.votacionId()));

        if (!votacion.estaAbierta()) {
            throw new VotacionCerradaException(votacion.getId());
        }

        Opcion opcion = opcionRepository.findByIdAndVotacionId(
                        request.opcionId(), request.votacionId())
                .orElseThrow(() -> new OpcionNoEncontradaException(
                        request.opcionId(), request.votacionId()));

        if (!opcion.estaActiva()) {
            throw new OpcionInactivaException(opcion.getId());
        }

        if (votoRepository.existsByVotacionIdAndUsuarioId(
                request.votacionId(), request.usuarioId())) {
            throw new VotoDuplicadoException(request.usuarioId(), request.votacionId());
        }

        try {
            Voto voto = votoRepository.save(
                    new Voto(votacion, opcion, request.usuarioId()));
            eventPublisher.publishEvent(new VotoEmitidoEvent(
                    UUID.randomUUID(),
                    voto.getId(),
                    votacion.getId(),
                    opcion.getId(),
                    voto.getUsuarioId(),
                    voto.getEmitidoEn()
            ));
            return toResponse(voto);
        } catch (DataIntegrityViolationException exception) {
            throw new VotoDuplicadoException(request.usuarioId(), request.votacionId());
        }
    }

    private VotoResponse toResponse(Voto voto) {
        return new VotoResponse(
                voto.getId(),
                voto.getVotacion().getId(),
                voto.getOpcion().getId(),
                voto.getUsuarioId(),
                voto.getEmitidoEn(),
                "Voto registrado correctamente"
        );
    }
}
