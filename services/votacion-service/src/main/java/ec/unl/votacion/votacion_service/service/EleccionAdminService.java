package ec.unl.votacion.votacion_service.service;

import ec.unl.votacion.votacion_service.dto.CandidatoAdminRequest;
import ec.unl.votacion.votacion_service.dto.CandidatoAdminResponse;
import ec.unl.votacion.votacion_service.dto.EleccionAdminRequest;
import ec.unl.votacion.votacion_service.dto.EleccionAdminResponse;
import ec.unl.votacion.votacion_service.entity.Candidato;
import ec.unl.votacion.votacion_service.entity.Eleccion;
import ec.unl.votacion.votacion_service.exception.CandidatoNoEncontradoException;
import ec.unl.votacion.votacion_service.exception.EleccionNoEncontradaException;
import ec.unl.votacion.votacion_service.exception.HorarioEleccionInvalidoException;
import ec.unl.votacion.votacion_service.repository.CandidatoRepository;
import ec.unl.votacion.votacion_service.repository.EleccionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EleccionAdminService {

    private final EleccionRepository eleccionRepository;
    private final CandidatoRepository candidatoRepository;

    public EleccionAdminService(EleccionRepository eleccionRepository,
                                CandidatoRepository candidatoRepository) {
        this.eleccionRepository = eleccionRepository;
        this.candidatoRepository = candidatoRepository;
    }

    @Transactional(readOnly = true)
    public List<EleccionAdminResponse> listar() {
        return eleccionRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public EleccionAdminResponse crear(EleccionAdminRequest request) {
        validarHorario(request);
        return toResponse(eleccionRepository.save(new Eleccion(
                request.titulo().trim(), request.fecha(), request.horaInicio(),
                request.horaFin(), request.estaActiva())));
    }

    @Transactional
    public EleccionAdminResponse actualizar(Long id, EleccionAdminRequest request) {
        validarHorario(request);
        Eleccion eleccion = buscarEleccion(id);
        eleccion.actualizar(request.titulo().trim(), request.fecha(), request.horaInicio(),
                request.horaFin(), request.estaActiva());
        return toResponse(eleccion);
    }

    @Transactional
    public CandidatoAdminResponse agregarCandidato(Long eleccionId, CandidatoAdminRequest request) {
        Eleccion eleccion = buscarEleccion(eleccionId);
        Candidato candidato = candidatoRepository.save(new Candidato(
                eleccion, request.nombre().trim(), request.apellido().trim(),
                request.partidoPolitico().trim()));
        return toCandidateResponse(candidato);
    }

    @Transactional
    public void eliminarCandidato(Long eleccionId, Long candidatoId) {
        Candidato candidato = candidatoRepository.findByIdAndEleccionId(candidatoId, eleccionId)
                .orElseThrow(() -> new CandidatoNoEncontradoException(candidatoId, eleccionId));
        candidatoRepository.delete(candidato);
    }

    @Transactional
    public void eliminar(Long id) {
        eleccionRepository.delete(buscarEleccion(id));
    }

    private Eleccion buscarEleccion(Long id) {
        return eleccionRepository.findById(id)
                .orElseThrow(() -> new EleccionNoEncontradaException(id));
    }

    private void validarHorario(EleccionAdminRequest request) {
        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw new HorarioEleccionInvalidoException();
        }
    }

    private EleccionAdminResponse toResponse(Eleccion eleccion) {
        List<CandidatoAdminResponse> candidatos = candidatoRepository
                .findByEleccionIdOrderByNombreAscApellidoAsc(eleccion.getId())
                .stream().map(this::toCandidateResponse).toList();
        return new EleccionAdminResponse(
                eleccion.getId(), eleccion.getTitulo(), eleccion.getFecha(),
                eleccion.getHoraInicio(), eleccion.getHoraFin(), eleccion.estaActiva(), candidatos);
    }

    private CandidatoAdminResponse toCandidateResponse(Candidato candidato) {
        return new CandidatoAdminResponse(candidato.getId(), candidato.getNombre(),
                candidato.getApellido(), candidato.getPartidoPolitico(), candidato.estaActivo());
    }
}
