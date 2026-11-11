package ec.unl.votacion.votacion_service.repository;

import ec.unl.votacion.votacion_service.entity.Voto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface VotoRepository extends JpaRepository<Voto, UUID> {

    boolean existsByEleccionIdAndIdUsuario(UUID eleccionId, UUID idUsuario);
}
