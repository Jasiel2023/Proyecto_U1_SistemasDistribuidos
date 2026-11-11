package ec.unl.votacion.usuarios.service;

import ec.unl.votacion.usuarios.dto.CrearUsuarioRequest;
import ec.unl.votacion.usuarios.dto.UsuarioResponse;
import ec.unl.votacion.usuarios.entity.Usuario;
import ec.unl.votacion.usuarios.exception.ConflictoException;
import ec.unl.votacion.usuarios.exception.RecursoNoEncontradoException;
import ec.unl.votacion.usuarios.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public UsuarioResponse crear(CrearUsuarioRequest request) {
        if (usuarioRepository.existsByIdCuenta(request.idCuenta())) {
            throw new ConflictoException("El idCuenta ya se encuentra registrado");
        }
        if (usuarioRepository.existsByCedula(request.cedula())) {
            throw new ConflictoException("La cedula ya se encuentra registrada");
        }
        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new ConflictoException("El correo ya se encuentra registrado");
        }
        if (usuarioRepository.existsByNickname(request.nickname())) {
            throw new ConflictoException("El nickname ya se encuentra registrado");
        }

        Usuario usuario = new Usuario(
                request.idCuenta(),
                request.cedula().trim(),
                request.correo().trim().toLowerCase(),
                request.nickname().trim(),
                request.nombre().trim(),
                request.apellido().trim()
        );

        usuario = usuarioRepository.save(usuario);
        return toResponse(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un usuario con el id " + id));
        return toResponse(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorIdCuenta(Integer idCuenta) {
        Usuario usuario = usuarioRepository.findByIdCuenta(idCuenta)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un usuario con el idCuenta " + idCuenta));
        return toResponse(usuario);
    }

    @Transactional(readOnly = true)
    public boolean existePorCedula(String cedula) {
        return usuarioRepository.existsByCedula(cedula.trim());
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(Pageable pageable) {
        return usuarioRepository.findAll(pageable).map(this::toResponse);
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getIdCuenta(),
                usuario.getCedula(),
                usuario.getCorreo(),
                usuario.getNickname(),
                usuario.getNombre(),
                usuario.getApellido()
        );
    }
}