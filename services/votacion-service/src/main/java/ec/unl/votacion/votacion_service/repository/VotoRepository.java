package ec.unl.votacion.votacion_service.repository;

import ec.unl.votacion.votacion_service.entity.Voto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VotoRepository extends JpaRepository<Voto, Long> {

    boolean existsByVotacionIdAndUsuarioId(Long votacionId, Long usuarioId);
}
