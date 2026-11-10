package ec.unl.votacion.autenticacion.repository;

import ec.unl.votacion.autenticacion.entity.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Integer> {
    Optional<Cuenta> findByNickname(String nickname);
    boolean existsByNickname(String nickname);
}
