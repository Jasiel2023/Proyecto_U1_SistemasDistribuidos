package ec.unl.votacion.votacion_service.repository;

import ec.unl.votacion.votacion_service.entity.Opcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OpcionRepository extends JpaRepository<Opcion, Long> {

    Optional<Opcion> findByIdAndVotacionId(Long id, Long votacionId);
}
