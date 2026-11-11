package ec.unl.votacion.autenticacion.repository;

import ec.unl.votacion.autenticacion.entity.Sesion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SesionRepository extends JpaRepository<Sesion, Long> {

    @EntityGraph(attributePaths = "cuenta")
    Optional<Sesion> findByToken(String token);

    boolean existsByToken(String token);
}
