package ec.unl.votacion.usuarios.dto;

public record UsuarioResponse(
        Integer id,
        Integer idCuenta,
        String cedula,
        String correo,
        String nickname,
        String nombre,
        String apellido,
        String rol
) {
}
