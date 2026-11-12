package ec.unl.votacion.autenticacion.dto;

import java.time.LocalDateTime;

public class UserResponse {

    private Integer userId;
    private String nickname;
    private LocalDateTime expiraEn;

    public UserResponse() {
    }

    public UserResponse(Integer userId, String nickname, LocalDateTime expiraEn) {
        this.userId = userId;
        this.nickname = nickname;
        this.expiraEn = expiraEn;
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
}
