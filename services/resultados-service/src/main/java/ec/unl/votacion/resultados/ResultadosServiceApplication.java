package ec.unl.votacion.resultados;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ResultadosServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResultadosServiceApplication.class, args);
    }
}
