package ec.unl.votacion.votacion_service.repository;

import ec.unl.votacion.votacion_service.entity.Votacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VotacionRepository extends JpaRepository<Votacion, Long> {
}
