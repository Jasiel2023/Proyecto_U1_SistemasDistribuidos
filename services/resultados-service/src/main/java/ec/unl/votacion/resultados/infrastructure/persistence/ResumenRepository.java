package ec.unl.votacion.resultados.infrastructure.persistence;

import ec.unl.votacion.resultados.domain.Resumen;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumenRepository extends JpaRepository<Resumen, UUID> {
}
