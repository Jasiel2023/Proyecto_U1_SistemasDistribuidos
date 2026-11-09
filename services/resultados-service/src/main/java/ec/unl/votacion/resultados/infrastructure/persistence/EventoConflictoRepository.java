package ec.unl.votacion.resultados.infrastructure.persistence;

import ec.unl.votacion.resultados.domain.EventoConflicto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoConflictoRepository extends JpaRepository<EventoConflicto, Long> {
}
