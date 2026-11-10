package ec.unl.votacion.autenticacion.dto;

public class RegisterResponse {

    private Integer userId;
    private String nickname;
    private String mensaje;

    public RegisterResponse() {
    }

    public RegisterResponse(Integer userId, String nickname, String mensaje) {
        this.userId = userId;
        this.nickname = nickname;
        this.mensaje = mensaje;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
