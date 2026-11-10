package ec.unl.votacion.autenticacion.dto;

import java.time.LocalDateTime;

public class ValidateResponse {

    private boolean valido;
    private Integer userId;
    private String nickname;
    private LocalDateTime expiraEn;
    private String mensaje;

    public ValidateResponse() {
    }

    public ValidateResponse(boolean valido, Integer userId, String nickname,
                            LocalDateTime expiraEn, String mensaje) {
        this.valido = valido;
        this.userId = userId;
        this.nickname = nickname;
        this.expiraEn = expiraEn;
        this.mensaje = mensaje;
    }

    public static ValidateResponse invalido(String mensaje) {
        return new ValidateResponse(false, null, null, null, mensaje);
    }

    public boolean isValido() {
        return valido;
    }

    public void setValido(boolean valido) {
        this.valido = valido;
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

    public LocalDateTime getExpiraEn() {
        return expiraEn;
    }

    public void setExpiraEn(LocalDateTime expiraEn) {
        this.expiraEn = expiraEn;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
