package ec.unl.votacion.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ec.unl.votacion.usuarios.entity.Rol;

public record CrearUsuarioRequest(
        @NotBlank(message = "La cedula es obligatoria")
        @Pattern(regexp = "\\d{10,13}", message = "La cedula debe contener entre 10 y 13 digitos")
        @Size(max = 13, message = "La cedula no puede superar los 13 caracteres")
        String cedula,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
        String correo,

        @NotBlank(message = "El nickname es obligatorio")
        @Size(max = 60, message = "El nickname no puede superar los 60 caracteres")
        String nickname,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
        String apellido,

        @NotBlank(message = "La clave es obligatoria")
        @Size(min = 6, max = 72, message = "La clave debe tener entre 6 y 72 caracteres")
        String clave,

        Rol rol
) {
}
