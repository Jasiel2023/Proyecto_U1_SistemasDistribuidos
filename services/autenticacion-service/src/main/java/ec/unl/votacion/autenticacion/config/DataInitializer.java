package ec.unl.votacion.autenticacion.config;

import ec.unl.votacion.autenticacion.entity.Cuenta;
import ec.unl.votacion.autenticacion.repository.CuentaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea una cuenta administradora inicial para poder probar el login
 * en un entorno de desarrollo con la base de datos vacia.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CuentaRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String username;
    private final String password;

    public DataInitializer(CuentaRepository cuentaRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.admin.enabled:true}") boolean enabled,
                           @Value("${app.admin.username:admin}") String username,
                           @Value("${app.admin.password:admin123}") String password) {
        this.cuentaRepository = cuentaRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }

        if (cuentaRepository.existsByNickname(username)) {
            log.info("La cuenta inicial '{}' ya existe; no se crea de nuevo", username);
            return;
        }

        Cuenta cuenta = new Cuenta();
        cuenta.setNickname(username);
        cuenta.setClaveCifrada(passwordEncoder.encode(password));
        cuenta.setEstaActiva(true);
        cuentaRepository.save(cuenta);

        log.warn("Cuenta inicial creada: usuario='{}'. Cambie APP_ADMIN_PASSWORD en produccion", username);
    }
}
