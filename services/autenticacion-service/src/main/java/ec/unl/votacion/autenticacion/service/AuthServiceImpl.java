package ec.unl.votacion.autenticacion.service;

import ec.unl.votacion.autenticacion.config.JwtProperties;
import ec.unl.votacion.autenticacion.dto.LoginRequest;
import ec.unl.votacion.autenticacion.dto.LoginResponse;
import ec.unl.votacion.autenticacion.dto.RegisterRequest;
import ec.unl.votacion.autenticacion.dto.RegisterResponse;
import ec.unl.votacion.autenticacion.dto.UserResponse;
import ec.unl.votacion.autenticacion.dto.ValidateResponse;
import ec.unl.votacion.autenticacion.entity.Cuenta;
import ec.unl.votacion.autenticacion.entity.EstadoToken;
import ec.unl.votacion.autenticacion.entity.Sesion;
import ec.unl.votacion.autenticacion.exception.CredencialesInvalidasException;
import ec.unl.votacion.autenticacion.exception.NicknameDuplicadoException;
import ec.unl.votacion.autenticacion.repository.CuentaRepository;
import ec.unl.votacion.autenticacion.repository.SesionRepository;
import io.jsonwebtoken.Jwts;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final CuentaRepository cuentaRepository;
    private final SesionRepository sesionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    public AuthServiceImpl(CuentaRepository cuentaRepository,
                           SesionRepository sesionRepository,
                           PasswordEncoder passwordEncoder,
                           JwtProperties jwtProperties) {
        this.cuentaRepository = cuentaRepository;
        this.sesionRepository = sesionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public RegisterResponse register(RegisterRequest request) {
        String nickname = request.getNickname().trim();

        if (cuentaRepository.existsByNickname(nickname)) {
            throw new NicknameDuplicadoException("El nickname '" + nickname + "' ya esta registrado");
        }

        Cuenta cuenta = new Cuenta();
        cuenta.setNickname(nickname);
        cuenta.setClaveCifrada(passwordEncoder.encode(request.getClave()));
        cuenta.setEstaActiva(true);

        Cuenta guardada = cuentaRepository.save(cuenta);
        return new RegisterResponse(guardada.getId(), guardada.getNickname(), "Cuenta registrada correctamente");
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        Cuenta cuenta = cuentaRepository.findByNickname(request.getNickname().trim())
                .orElseThrow(() -> new CredencialesInvalidasException("Credenciales invalidas"));

        if (!cuenta.isEstaActiva()) {
            throw new CredencialesInvalidasException("La cuenta esta inactiva");
        }

        if (!passwordEncoder.matches(request.getClave(), cuenta.getClaveCifrada())) {
            throw new CredencialesInvalidasException("Credenciales invalidas");
        }

        Instant ahoraInstant = Instant.now();
        LocalDateTime ahora = LocalDateTime.ofInstant(ahoraInstant, ZoneId.systemDefault());
        LocalDateTime expira = ahora.plus(Duration.ofMillis(jwtProperties.getExpirationMs()));
        String token = generarJwt(cuenta, ahoraInstant, expira);

        Sesion sesion = cuenta.getSesion();
        if (sesion == null) {
            sesion = new Sesion();
            sesion.setCuenta(cuenta);
        }
        sesion.setToken(token);
        sesion.setFechaEmision(ahora);
        sesion.setFechaExpiracion(expira);
        sesion.setEstadoToken(EstadoToken.ACTIVO);
        cuenta.setSesion(sesion);

        cuentaRepository.save(cuenta);

        return new LoginResponse(
                true,
                token,
                TOKEN_TYPE,
                jwtProperties.getExpirationMs() / 1000,
                cuenta.getNickname(),
                "Login exitoso");
    }

    @Override
    public ValidateResponse validateToken(String token) {
        Sesion sesion = sesionRepository.findByToken(token)
                .orElseThrow(() -> new CredencialesInvalidasException("Token no encontrado"));

        if (sesion.getEstadoToken() != EstadoToken.ACTIVO) {
            return ValidateResponse.invalido("Token inactivo");
        }

        if (sesion.getFechaExpiracion() == null || sesion.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            sesion.setEstadoToken(EstadoToken.EXPIRADO);
            sesionRepository.save(sesion);
            return ValidateResponse.invalido("Token expirado");
        }

        try {
            Jwts.parserBuilder()
                    .setSigningKey(jwtProperties.signingKey())
                    .build()
                    .parseClaimsJws(token);
        } catch (Exception ex) {
            return ValidateResponse.invalido("Token invalido");
        }

        Cuenta cuenta = sesion.getCuenta();
        if (cuenta == null || !cuenta.isEstaActiva()) {
            return ValidateResponse.invalido("La cuenta esta inactiva");
        }

        return new ValidateResponse(
                true,
                cuenta.getId(),
                cuenta.getNickname(),
                sesion.getFechaExpiracion(),
                "Token valido");
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse me(String token) {
        Sesion sesion = sesionRepository.findByToken(token)
                .orElseThrow(() -> new CredencialesInvalidasException("Token no encontrado"));

        if (sesion.getEstadoToken() != EstadoToken.ACTIVO
                || sesion.getFechaExpiracion() == null
                || sesion.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new CredencialesInvalidasException("Token invalido o expirado");
        }

        Cuenta cuenta = sesion.getCuenta();
        if (cuenta == null || !cuenta.isEstaActiva()) {
            throw new CredencialesInvalidasException("La cuenta esta inactiva");
        }

        return new UserResponse(cuenta.getId(), cuenta.getNickname(), sesion.getFechaExpiracion());
    }

    @Override
    public boolean logout(String token) {
        Sesion sesion = sesionRepository.findByToken(token)
                .orElseThrow(() -> new CredencialesInvalidasException("Token no encontrado"));

        sesion.setEstadoToken(EstadoToken.EXPIRADO);
        sesionRepository.save(sesion);
        return true;
    }

    private String generarJwt(Cuenta cuenta, Instant ahora, LocalDateTime expira) {
        Date expiracion = Date.from(expira.atZone(ZoneId.systemDefault()).toInstant());
        return Jwts.builder()
                .setSubject(cuenta.getNickname())
                .claim("userId", cuenta.getId())
                .claim("nickname", cuenta.getNickname())
                .setIssuedAt(Date.from(ahora))
                .setExpiration(expiracion)
                .signWith(jwtProperties.signingKey())
                .compact();
    }
}
