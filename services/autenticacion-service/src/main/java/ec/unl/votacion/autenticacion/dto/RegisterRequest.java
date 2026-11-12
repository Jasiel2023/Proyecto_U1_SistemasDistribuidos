package ec.unl.votacion.autenticacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "El nickname es obligatorio")
    @Size(min = 3, max = 100, message = "El nickname debe tener entre 3 y 100 caracteres")
    private String nickname;

    @NotBlank(message = "La clave es obligatoria")
    @Size(min = 6, max = 72, message = "La clave debe tener entre 6 y 72 caracteres")
    private String clave;

    public RegisterRequest() {
    }

    public RegisterRequest(String nickname, String clave) {
        this.nickname = nickname;
        this.clave = clave;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }
}
