package ec.unl.votacion.usuarios.service;

import ec.unl.votacion.usuarios.dto.CrearUsuarioRequest;
import ec.unl.votacion.usuarios.dto.UsuarioResponse;
import ec.unl.votacion.usuarios.entity.Rol;
import ec.unl.votacion.usuarios.entity.Usuario;
import ec.unl.votacion.usuarios.exception.ConflictoException;
import ec.unl.votacion.usuarios.exception.RecursoNoEncontradoException;
import ec.unl.votacion.usuarios.messaging.event.CuentaCrearEvent;
import ec.unl.votacion.usuarios.messaging.event.CuentaCreadaEvent;
import ec.unl.votacion.usuarios.messaging.consumer.CuentaEventConsumer;
import ec.unl.votacion.usuarios.messaging.publisher.CuentaEventPublisher;
import ec.unl.votacion.usuarios.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class UsuarioService {

    private static final long TIEMPO_ESPERA_SEGUNDOS = 15;

    private final UsuarioRepository usuarioRepository;
    private final CuentaEventPublisher cuentaEventPublisher;
    private final CuentaEventConsumer cuentaEventConsumer;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          CuentaEventPublisher cuentaEventPublisher,
                          CuentaEventConsumer cuentaEventConsumer) {
        this.usuarioRepository = usuarioRepository;
        this.cuentaEventPublisher = cuentaEventPublisher;
        this.cuentaEventConsumer = cuentaEventConsumer;
    }

    @Transactional
    public UsuarioResponse crear(CrearUsuarioRequest request) {
        // Validar campos únicos antes de crear la cuenta
        if (usuarioRepository.existsByCedula(request.cedula())) {
            throw new ConflictoException("La cedula ya se encuentra registrada");
        }
        if (usuarioRepository.existsByCorreo(request.correo())) {
            throw new ConflictoException("El correo ya se encuentra registrada");
        }
        if (usuarioRepository.existsByNickname(request.nickname())) {
            throw new ConflictoException("El nickname ya se encuentra registrado");
        }

        // Registrar futuro pendiente para esperar la respuesta de autenticacion-service
        CompletableFuture<CuentaCreadaEvent> futuro = new CompletableFuture<>();
        cuentaEventConsumer.registrarPendiente(request.nickname(), futuro);

        // Publicar solicitud de crear cuenta en autenticacion-service
        CuentaCrearEvent evento = new CuentaCrearEvent(
                request.nickname(),
                request.clave(),
                request.correo(),
                request.rol() != null ? request.rol().name() : "VOTANTE"
        );
        cuentaEventPublisher.publicarSolicitudCrearCuenta(evento);

        // Esperar la respuesta
        CuentaCreadaEvent respuesta;
        try {
            respuesta = futuro.get(TIEMPO_ESPERA_SEGUNDOS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConflictoException("La solicitud de crear cuenta fue interrumpida");
        } catch (ExecutionException e) {
            throw new ConflictoException("Error al crear la cuenta: " + e.getMessage());
        } catch (TimeoutException e) {
            throw new ConflictoException("Tiempo de espera agotado al crear la cuenta");
        }

        if (!respuesta.exito()) {
            throw new ConflictoException(respuesta.mensaje() != null ? respuesta.mensaje() : "No se pudo crear la cuenta");
        }

        Integer idCuenta = respuesta.idCuenta();

        // Verificar que el idCuenta no ya exista en usuario-service
        if (usuarioRepository.existsByIdCuenta(idCuenta)) {
            throw new ConflictoException("El idCuenta ya se encuentra registrado en usuarios");
        }

        Rol rol = request.rol() != null ? request.rol() : Rol.VOTANTE;

        Usuario usuario = new Usuario(
                idCuenta,
                request.cedula().trim(),
                request.correo().trim().toLowerCase(),
                request.nickname().trim(),
                request.nombre().trim(),
                request.apellido().trim(),
                rol
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
                usuario.getApellido(),
                usuario.getRol().name()
        );
    }
}
