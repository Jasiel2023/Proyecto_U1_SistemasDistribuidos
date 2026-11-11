package ec.unl.votacion.autenticacion.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @NotBlank
    private String nickname;
    @NotBlank
    private String clave;

    public LoginRequest() {}

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
}
