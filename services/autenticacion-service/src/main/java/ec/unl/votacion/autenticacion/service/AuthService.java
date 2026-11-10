package ec.unl.votacion.autenticacion.service;

import ec.unl.votacion.autenticacion.dto.LoginRequest;
import ec.unl.votacion.autenticacion.dto.LoginResponse;
import ec.unl.votacion.autenticacion.dto.RegisterRequest;
import ec.unl.votacion.autenticacion.dto.RegisterResponse;
import ec.unl.votacion.autenticacion.dto.UserResponse;
import ec.unl.votacion.autenticacion.dto.ValidateResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    ValidateResponse validateToken(String token);

    UserResponse me(String token);

    boolean logout(String token);
}
