package ec.unl.votacion.resultados.infrastructure.persistence;

import ec.unl.votacion.resultados.domain.ConteoCandidato;
import ec.unl.votacion.resultados.domain.ConteoCandidatoId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConteoCandidatoRepository extends JpaRepository<ConteoCandidato, ConteoCandidatoId> {

    List<ConteoCandidato> findByIdEleccionIdOrderByIdCandidatoIdAsc(Long eleccionId);
}
