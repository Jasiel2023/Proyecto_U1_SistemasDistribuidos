package ec.unl.votacion.autenticacion.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Propiedades de configuracion del JWT, enlazadas desde application.yml
 * con el prefijo {@code jwt}.
 */
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private static final int MIN_SECRET_BYTES = 32;

    private String secret;
    private long expirationMs = 3_600_000L;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }

    /**
     * Construye la llave HMAC a partir del secreto configurado.
     * HS256 exige un secreto de al menos 256 bits (32 bytes).
     */
    public SecretKey signingKey() {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret debe tener al menos " + MIN_SECRET_BYTES + " bytes (256 bits) para HS256");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
