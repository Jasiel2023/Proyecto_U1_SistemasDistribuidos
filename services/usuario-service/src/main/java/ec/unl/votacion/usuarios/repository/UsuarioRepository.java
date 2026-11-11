package ec.unl.votacion.usuarios.repository;

import ec.unl.votacion.usuarios.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByIdCuenta(Integer idCuenta);

    Optional<Usuario> findByCedula(String cedula);

    boolean existsByCedula(String cedula);

    boolean existsByCorreo(String correo);

    boolean existsByNickname(String nickname);

    boolean existsByIdCuenta(Integer idCuenta);

    Page<Usuario> findAll(Pageable pageable);
}