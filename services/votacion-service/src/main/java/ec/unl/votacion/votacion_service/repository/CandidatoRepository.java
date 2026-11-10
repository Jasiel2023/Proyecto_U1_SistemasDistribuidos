package ec.unl.votacion.votacion_service.repository;

import ec.unl.votacion.votacion_service.entity.Candidato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface CandidatoRepository extends JpaRepository<Candidato, Long> {

    Optional<Candidato> findByIdAndEleccionId(Long id, Long eleccionId);

    List<Candidato> findByEleccionIdAndActivoTrueOrderByNombreAscApellidoAsc(Long eleccionId);

    List<Candidato> findByEleccionIdOrderByNombreAscApellidoAsc(Long eleccionId);
}
