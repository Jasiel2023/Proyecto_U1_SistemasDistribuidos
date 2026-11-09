package ec.unl.votacion.resultados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.unl.votacion.resultados.messaging.VotoEmitidoEvent;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

class VotoEmitidoContractTest {

    private static final Path EJEMPLOS = Path.of("..", "..", "specs", "001-resultados",
            "contracts", "examples");

    private final JsonMapper mapper = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void aceptaEjemploValidoDelContrato() throws Exception {
        String json = Files.readString(EJEMPLOS.resolve("voto-emitido-valid.json"));
        VotoEmitidoEvent evento = mapper.readValue(json, VotoEmitidoEvent.class);

        assertThat(evento.schemaVersion()).isEqualTo(1);
        assertThat(evento.eleccionId().toString())
                .isEqualTo("4f1c1a1e-6f31-4f7e-9f0a-2f4d0f6a1b12");
        assertThat(validator.validate(evento)).isEmpty();
    }

    @Test
    void rechazaEventoSinCandidato() throws Exception {
        String json = Files.readString(EJEMPLOS.resolve("voto-emitido-invalid-missing-candidate.json"));
        VotoEmitidoEvent evento = mapper.readValue(json, VotoEmitidoEvent.class);

        assertThat(validator.validate(evento)).isNotEmpty();
    }

    @Test
    void rechazaVersionNoSoportada() throws Exception {
        String json = Files.readString(EJEMPLOS.resolve("voto-emitido-invalid-version.json"));
        VotoEmitidoEvent evento = mapper.readValue(json, VotoEmitidoEvent.class);

        assertThat(validator.validate(evento)).isNotEmpty();
    }

    @Test
    void rechazaCampoAdicionalNoPermitido() throws Exception {
        String json = Files.readString(EJEMPLOS.resolve("voto-emitido-invalid-extra-field.json"));

        assertThatThrownBy(() -> mapper.readValue(json, VotoEmitidoEvent.class))
                .isInstanceOf(tools.jackson.core.JacksonException.class);
    }
}
