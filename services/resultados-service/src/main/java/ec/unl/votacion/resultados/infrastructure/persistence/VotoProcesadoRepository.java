package ec.unl.votacion.resultados.infrastructure.persistence;

import ec.unl.votacion.resultados.domain.VotoProcesado;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VotoProcesadoRepository extends JpaRepository<VotoProcesado, UUID> {

    Optional<VotoProcesado> findByEleccionIdAndVotoId(Long eleccionId, Long votoId);
}
