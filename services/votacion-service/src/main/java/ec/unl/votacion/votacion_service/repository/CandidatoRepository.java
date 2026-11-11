package ec.unl.votacion.votacion_service.repository;

import ec.unl.votacion.votacion_service.entity.Candidato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface CandidatoRepository extends JpaRepository<Candidato, UUID> {

    Optional<Candidato> findByIdAndEleccionId(UUID id, UUID eleccionId);

    List<Candidato> findByEleccionIdAndActivoTrueOrderByNombreAscApellidoAsc(UUID eleccionId);

    List<Candidato> findByEleccionIdOrderByNombreAscApellidoAsc(UUID eleccionId);
}
