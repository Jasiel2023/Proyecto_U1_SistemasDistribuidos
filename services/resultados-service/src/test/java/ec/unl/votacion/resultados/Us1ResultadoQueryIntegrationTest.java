package ec.unl.votacion.resultados;

import static org.assertj.core.api.Assertions.assertThat;

import ec.unl.votacion.resultados.application.ResultadoSnapshot;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class Us1ResultadoQueryIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int puerto;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final JsonMapper mapper = JsonMapper.builder().build();
    private HttpClient http;
    private String base;

    @BeforeEach
    void limpiar() {
        jdbcTemplate.execute("TRUNCATE TABLE evento_conflicto, voto_procesado, "
                + "resultado_conteo, resultado_resumen RESTART IDENTITY CASCADE");
        http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        base = "http://localhost:" + puerto;
    }

    @Test
    void devuelveSnapshotVacioSinProyeccion() throws Exception {
        Long eleccion = 1L;

        HttpResponse<String> respuesta = obtener(eleccion);

        assertThat(respuesta.statusCode()).isEqualTo(200);
        ResultadoSnapshot snapshot = mapper.readValue(respuesta.body(), ResultadoSnapshot.class);
        assertThat(snapshot.eleccionId()).isEqualTo(eleccion);
        assertThat(snapshot.totalVotos()).isZero();
        assertThat(snapshot.conteos()).isEmpty();
        assertThat(snapshot.revision()).isZero();
        assertThat(snapshot.ultimaActualizacion()).isNull();
        assertThat(snapshot.disponible()).isFalse();
    }

    @Test
    void devuelveConteosYRevisionCoherentes() throws Exception {
        Long eleccion = 1L;
        Long candidatoA = 2L;
        Long candidatoB = 3L;
        OffsetDateTime instante = OffsetDateTime.of(2026, 11, 8, 15, 4, 6, 123_000_000, ZoneOffset.UTC);
        sembrar(eleccion, 3, 4, instante, candidatoA, 2, candidatoB, 1);

        ResultadoSnapshot snapshot = mapper.readValue(obtener(eleccion).body(), ResultadoSnapshot.class);

        assertThat(snapshot.disponible()).isTrue();
        assertThat(snapshot.totalVotos()).isEqualTo(3);
        assertThat(snapshot.revision()).isEqualTo(4);
        assertThat(snapshot.ultimaActualizacion()).isEqualTo(instante);
        assertThat(snapshot.conteos()).hasSize(2);
        assertThat(snapshot.conteos()).extracting(ResultadoSnapshot.ConteoSnapshot::votos)
                .containsExactlyInAnyOrder(2L, 1L);
        assertThat(snapshot.conteos().stream().mapToLong(ResultadoSnapshot.ConteoSnapshot::votos).sum())
                .isEqualTo(snapshot.totalVotos());
    }

    @Test
    void aislaElecciones() throws Exception {
        Long eleccionA = 1L;
        Long eleccionB = 2L;
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        sembrar(eleccionA, 2, 2, ahora, 3L, 2);
        sembrar(eleccionB, 5, 5, ahora, 4L, 5);

        ResultadoSnapshot snapshotA = mapper.readValue(obtener(eleccionA).body(), ResultadoSnapshot.class);

        assertThat(snapshotA.totalVotos()).isEqualTo(2);
        assertThat(snapshotA.conteos()).hasSize(1);
        assertThat(snapshotA.conteos().getFirst().votos()).isEqualTo(2);
    }

    @Test
    void rechazaIdInvalidoConProblemDetails() throws Exception {
        HttpResponse<String> respuesta = get("/api/resultados/elecciones/no-es-numero");

        assertThat(respuesta.statusCode()).isEqualTo(400);
        assertThat(respuesta.headers().firstValue("Content-Type").orElse(""))
                .startsWith("application/problem+json");
        assertThat(respuesta.body()).contains("Solicitud invalida");
    }

    @Test
    void noExponeIdentidadDeVotantesNiEntidades() throws Exception {
        Long eleccion = 1L;
        sembrar(eleccion, 1, 1, OffsetDateTime.now(ZoneOffset.UTC), 2L, 1);

        HttpResponse<String> respuesta = obtener(eleccion);

        assertThat(respuesta.body()).doesNotContain("votoId", "votante", "identificacion");
        JsonNode json = mapper.readTree(respuesta.body());
        assertThat(json.propertyNames()).containsExactlyInAnyOrder(
                "eleccionId", "totalVotos", "conteos", "revision",
                "ultimaActualizacion", "disponible");
    }

    private HttpResponse<String> obtener(Long eleccion) throws Exception {
        return get("/api/resultados/elecciones/" + eleccion);
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + path))
                .GET()
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void sembrar(Long eleccion, long total, long revision, OffsetDateTime instante,
                         Long candidato, long votos) {
        sembrar(eleccion, total, revision, instante, candidato, votos, null, 0);
    }

    private void sembrar(Long eleccion, long total, long revision, OffsetDateTime instante,
                         Long candidatoA, long votosA, Long candidatoB, long votosB) {
        jdbcTemplate.update("INSERT INTO resultado_resumen "
                        + "(eleccion_id, total_votos, revision, ultima_actualizacion) VALUES (?, ?, ?, ?)",
                eleccion, total, revision, instante);
        jdbcTemplate.update("INSERT INTO resultado_conteo "
                        + "(eleccion_id, candidato_id, votos, ultima_actualizacion) VALUES (?, ?, ?, ?)",
                eleccion, candidatoA, votosA, instante);
        if (candidatoB != null) {
            jdbcTemplate.update("INSERT INTO resultado_conteo "
                            + "(eleccion_id, candidato_id, votos, ultima_actualizacion) VALUES (?, ?, ?, ?)",
                    eleccion, candidatoB, votosB, instante);
        }
    }
}
