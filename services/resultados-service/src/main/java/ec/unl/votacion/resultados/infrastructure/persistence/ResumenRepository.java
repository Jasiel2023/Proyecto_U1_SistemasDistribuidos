package ec.unl.votacion.resultados.infrastructure.persistence;

import ec.unl.votacion.resultados.domain.Resumen;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ResumenRepository extends JpaRepository<Resumen, UUID> {
}
