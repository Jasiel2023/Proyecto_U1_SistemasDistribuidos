package ec.unl.votacion.autenticacion.dto;

public class LoginResponse {

    private boolean success;
    private String token;
    private String tokenType;
    private long expiresIn;
    private String nickname;
    private String mensaje;

    public LoginResponse() {
    }

    public LoginResponse(boolean success, String token, String tokenType, long expiresIn,
                         String nickname, String mensaje) {
        this.success = success;
        this.token = token;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.nickname = nickname;
        this.mensaje = mensaje;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
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
