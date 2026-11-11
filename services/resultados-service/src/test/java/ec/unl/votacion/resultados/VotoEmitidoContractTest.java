package ec.unl.votacion.resultados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.unl.votacion.resultados.messaging.VotoEmitidoEvent;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

class VotoEmitidoContractTest {

    private final JsonMapper mapper = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void aceptaEjemploValidoDelContrato() throws Exception {
        String json = leer("voto-emitido-valid.json");
        VotoEmitidoEvent evento = mapper.readValue(json, VotoEmitidoEvent.class);

        assertThat(evento.schemaVersion()).isEqualTo(1);
        assertThat(evento.eleccionId()).isEqualTo(1L);
        assertThat(validator.validate(evento)).isEmpty();
    }

    @Test
    void rechazaEventoSinCandidato() throws Exception {
        String json = leer("voto-emitido-invalid-missing-candidate.json");
        VotoEmitidoEvent evento = mapper.readValue(json, VotoEmitidoEvent.class);

        assertThat(validator.validate(evento)).isNotEmpty();
    }

    @Test
    void rechazaVersionNoSoportada() throws Exception {
        String json = leer("voto-emitido-invalid-version.json");
        VotoEmitidoEvent evento = mapper.readValue(json, VotoEmitidoEvent.class);

        assertThat(validator.validate(evento)).isNotEmpty();
    }

    @Test
    void rechazaCampoAdicionalNoPermitido() throws Exception {
        String json = leer("voto-emitido-invalid-extra-field.json");

        assertThatThrownBy(() -> mapper.readValue(json, VotoEmitidoEvent.class))
                .isInstanceOf(tools.jackson.core.JacksonException.class);
    }

    private static String leer(String nombre) throws IOException {
        try (InputStream entrada = VotoEmitidoContractTest.class.getResourceAsStream(
                "/contracts/examples/" + nombre)) {
            if (entrada == null) {
                throw new IllegalStateException("No se encontró el recurso " + nombre);
            }
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
