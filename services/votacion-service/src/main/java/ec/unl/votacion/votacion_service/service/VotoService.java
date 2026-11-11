package ec.unl.votacion.votacion_service.service;

import ec.unl.votacion.votacion_service.dto.CrearVotoRequest;
import ec.unl.votacion.votacion_service.dto.CandidatoResponse;
import ec.unl.votacion.votacion_service.dto.EleccionResponse;
import ec.unl.votacion.votacion_service.dto.VotoResponse;
import ec.unl.votacion.votacion_service.entity.Candidato;
import ec.unl.votacion.votacion_service.entity.Eleccion;
import ec.unl.votacion.votacion_service.entity.Voto;
import ec.unl.votacion.votacion_service.exception.CandidatoInactivoException;
import ec.unl.votacion.votacion_service.exception.CandidatoNoEncontradoException;
import ec.unl.votacion.votacion_service.exception.EleccionCerradaException;
import ec.unl.votacion.votacion_service.exception.EleccionNoEncontradaException;
import ec.unl.votacion.votacion_service.exception.VotoDuplicadoException;
import ec.unl.votacion.votacion_service.messaging.VotoEmitidoEvent;
import ec.unl.votacion.votacion_service.repository.CandidatoRepository;
import ec.unl.votacion.votacion_service.repository.EleccionRepository;
import ec.unl.votacion.votacion_service.repository.VotoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;
import java.time.ZoneOffset;

@Service
public class VotoService {

    private final EleccionRepository eleccionRepository;
    private final CandidatoRepository candidatoRepository;
    private final VotoRepository votoRepository;
    private final ApplicationEventPublisher eventPublisher;

    public VotoService(
            EleccionRepository eleccionRepository,
            CandidatoRepository candidatoRepository,
            VotoRepository votoRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.eleccionRepository = eleccionRepository;
        this.candidatoRepository = candidatoRepository;
        this.votoRepository = votoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public VotoResponse registrarVoto(CrearVotoRequest request) {
        Eleccion eleccion = eleccionRepository.findById(request.eleccionId())
                .orElseThrow(() -> new EleccionNoEncontradaException(request.eleccionId()));

        if (!eleccion.estaActiva()) {
            throw new EleccionCerradaException(eleccion.getId());
        }

        Candidato candidato = candidatoRepository.findByIdAndEleccionId(
                        request.candidatoId(), request.eleccionId())
                .orElseThrow(() -> new CandidatoNoEncontradoException(
                        request.candidatoId(), request.eleccionId()));

        if (!candidato.estaActivo()) {
            throw new CandidatoInactivoException(candidato.getId());
        }

        if (votoRepository.existsByEleccionIdAndIdUsuario(
                request.eleccionId(), request.idUsuario())) {
            throw new VotoDuplicadoException(request.idUsuario(), request.eleccionId());
        }

        try {
            Voto voto = votoRepository.save(
                    new Voto(eleccion, candidato, request.idUsuario()));
            eventPublisher.publishEvent(new VotoEmitidoEvent(
                    UUID.randomUUID(),
                    1,
                    voto.getId(),
                    eleccion.getId(),
                    candidato.getId(),
                    voto.getIdUsuario(),
                    voto.getEmitidoEn().atOffset(ZoneOffset.UTC)
            ));
            return toResponse(voto);
        } catch (DataIntegrityViolationException exception) {
            throw new VotoDuplicadoException(request.idUsuario(), request.eleccionId());
        }
    }

    @Transactional(readOnly = true)
    public List<EleccionResponse> listarEleccionesActivas() {
        return eleccionRepository.findByEstaActivaTrueOrderByFechaAscHoraInicioAsc().stream()
                .map(eleccion -> new EleccionResponse(
                        eleccion.getId(),
                        eleccion.getTitulo(),
                        eleccion.getFecha(),
                        eleccion.getHoraInicio(),
                        eleccion.getHoraFin(),
                        candidatoRepository.findByEleccionIdAndActivoTrueOrderByNombreAscApellidoAsc(
                                        eleccion.getId())
                                .stream()
                                .map(candidato -> new CandidatoResponse(
                                        candidato.getId(),
                                        candidato.getNombre(),
                                        candidato.getApellido(),
                                        candidato.getPartidoPolitico()))
                                .toList()))
                .toList();
    }

    private VotoResponse toResponse(Voto voto) {
        return new VotoResponse(
                voto.getId(),
                voto.getEleccion().getId(),
                voto.getCandidato().getId(),
                voto.getIdUsuario(),
                voto.getEmitidoEn(),
                "Voto registrado correctamente"
        );
    }
}
