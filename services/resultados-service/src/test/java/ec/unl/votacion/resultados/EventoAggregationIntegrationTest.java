package ec.unl.votacion.resultados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.unl.votacion.resultados.application.ResultadoCommandService;
import ec.unl.votacion.resultados.application.ResultadoQueryService;
import ec.unl.votacion.resultados.application.ResultadoSnapshot;
import ec.unl.votacion.resultados.messaging.VotoEmitidoEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class EventoAggregationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ResultadoCommandService commandService;

    @Autowired
    private ResultadoQueryService queryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void limpiar() {
        jdbcTemplate.execute("TRUNCATE TABLE evento_conflicto, voto_procesado, "
                + "resultado_conteo, resultado_resumen RESTART IDENTITY CASCADE");
    }

    @AfterEach
    void limpiarTrigger() {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_fallo_conteo ON resultado_conteo");
        jdbcTemplate.execute("DROP FUNCTION IF EXISTS fallo_conteo()");
    }

    @Test
    void eventoNuevoIncrementaResumenYConteo() {
        UUID eleccion = UUID.randomUUID();
        UUID candidato = UUID.randomUUID();

        commandService.procesar(TestEventos.voto(eleccion, candidato));

        ResultadoSnapshot snapshot = queryService.obtener(eleccion);
        assertThat(snapshot.disponible()).isTrue();
        assertThat(snapshot.totalVotos()).isEqualTo(1);
        assertThat(snapshot.revision()).isEqualTo(1);
        assertThat(snapshot.conteos()).hasSize(1);
        assertThat(snapshot.conteos().getFirst().candidatoId()).isEqualTo(candidato);
        assertThat(snapshot.conteos().getFirst().votos()).isEqualTo(1);
    }

    @Test
    void mismoEventIdNoIncrementa() {
        UUID eleccion = UUID.randomUUID();
        UUID candidato = UUID.randomUUID();
        VotoEmitidoEvent evento = TestEventos.voto(eleccion, candidato);

        commandService.procesar(evento);
        commandService.procesar(evento);

        assertThat(queryService.obtener(eleccion).totalVotos()).isEqualTo(1);
        assertThat(contar("voto_procesado")).isEqualTo(1);
        assertThat(contar("evento_conflicto")).isZero();
    }

    @Test
    void mismoVotoLogicoConOtroEventIdNoIncrementa() {
        UUID eleccion = UUID.randomUUID();
        UUID candidato = UUID.randomUUID();
        UUID votoId = UUID.randomUUID();
        commandService.procesar(TestEventos.voto(UUID.randomUUID(), votoId, eleccion, candidato));

        commandService.procesar(TestEventos.voto(UUID.randomUUID(), votoId, eleccion, candidato));

        assertThat(queryService.obtener(eleccion).totalVotos()).isEqualTo(1);
        assertThat(contar("voto_procesado")).isEqualTo(1);
        assertThat(contar("evento_conflicto")).isZero();
    }

    @Test
    void mismoEventIdConPayloadIncompatibleRegistraConflictoSinSumar() {
        UUID eleccion = UUID.randomUUID();
        UUID candidatoA = UUID.randomUUID();
        UUID candidatoB = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID votoId = UUID.randomUUID();

        commandService.procesar(TestEventos.voto(eventId, votoId, eleccion, candidatoA));
        commandService.procesar(TestEventos.voto(eventId, votoId, eleccion, candidatoB));

        ResultadoSnapshot snapshot = queryService.obtener(eleccion);
        assertThat(snapshot.totalVotos()).isEqualTo(1);
        assertThat(snapshot.conteos()).extracting(ResultadoSnapshot.ConteoSnapshot::candidatoId)
                .containsExactly(candidatoA);
        assertThat(contar("evento_conflicto")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT motivo FROM evento_conflicto", String.class))
                .isEqualTo("EVENT_ID_PAYLOAD_INCOMPATIBLE");
    }

    @Test
    void mismoVotoLogicoConCandidatoDistintoRegistraConflicto() {
        UUID eleccion = UUID.randomUUID();
        UUID votoId = UUID.randomUUID();
        UUID candidatoA = UUID.randomUUID();
        UUID candidatoB = UUID.randomUUID();

        commandService.procesar(TestEventos.voto(UUID.randomUUID(), votoId, eleccion, candidatoA));
        commandService.procesar(TestEventos.voto(UUID.randomUUID(), votoId, eleccion, candidatoB));

        ResultadoSnapshot snapshot = queryService.obtener(eleccion);
        assertThat(snapshot.totalVotos()).isEqualTo(1);
        assertThat(snapshot.conteos()).extracting(ResultadoSnapshot.ConteoSnapshot::candidatoId)
                .containsExactly(candidatoA);
        assertThat(contar("evento_conflicto")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT motivo FROM evento_conflicto", String.class))
                .isEqualTo("VOTO_LOGICO_PAYLOAD_INCOMPATIBLE");
    }

    @Test
    void aislaDosElecciones() {
        UUID eleccionA = UUID.randomUUID();
        UUID eleccionB = UUID.randomUUID();
        UUID candidato = UUID.randomUUID();

        commandService.procesar(TestEventos.voto(eleccionA, candidato));
        commandService.procesar(TestEventos.voto(eleccionA, candidato));
        commandService.procesar(TestEventos.voto(eleccionB, candidato));

        assertThat(queryService.obtener(eleccionA).totalVotos()).isEqualTo(2);
        assertThat(queryService.obtener(eleccionB).totalVotos()).isEqualTo(1);
    }

    @Test
    void mantieneTotalIgualASumaDeConteos() {
        UUID eleccion = UUID.randomUUID();
        UUID candidatoA = UUID.randomUUID();
        UUID candidatoB = UUID.randomUUID();

        for (int i = 0; i < 5; i++) {
            commandService.procesar(TestEventos.voto(eleccion, candidatoA));
        }
        for (int i = 0; i < 7; i++) {
            commandService.procesar(TestEventos.voto(eleccion, candidatoB));
        }

        ResultadoSnapshot snapshot = queryService.obtener(eleccion);
        long suma = snapshot.conteos().stream()
                .mapToLong(ResultadoSnapshot.ConteoSnapshot::votos).sum();
        assertThat(snapshot.totalVotos()).isEqualTo(12);
        assertThat(suma).isEqualTo(snapshot.totalVotos());
        assertThat(snapshot.revision()).isEqualTo(12);
    }

    @Test
    void concurrenciaYDuplicadosMantienenConteosExactos() throws Exception {
        UUID eleccion = UUID.randomUUID();
        UUID candidato = UUID.randomUUID();
        int unicos = 40;
        List<VotoEmitidoEvent> eventos = new ArrayList<>();
        for (int i = 0; i < unicos; i++) {
            eventos.add(TestEventos.voto(eleccion, candidato));
        }

        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<?>> futuros = new ArrayList<>();
        try {
            for (VotoEmitidoEvent evento : eventos) {
                for (int repeticion = 0; repeticion < 2; repeticion++) {
                    futuros.add(pool.submit(() -> {
                        salida.await();
                        commandService.procesar(evento);
                        return null;
                    }));
                }
            }
            salida.countDown();
            for (Future<?> futuro : futuros) {
                futuro.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        ResultadoSnapshot snapshot = queryService.obtener(eleccion);
        assertThat(snapshot.totalVotos()).isEqualTo(unicos);
        assertThat(snapshot.conteos().getFirst().votos()).isEqualTo(unicos);
        assertThat(snapshot.revision()).isEqualTo(unicos);
        assertThat(contar("voto_procesado")).isEqualTo(unicos);
    }

    @Test
    void rollbackNoDejaEstadoParcial() {
        UUID eleccion = UUID.randomUUID();
        UUID candidato = UUID.randomUUID();
        crearTriggerDeFallo(candidato);

        assertThatThrownBy(() -> commandService.procesar(TestEventos.voto(eleccion, candidato)))
                .hasRootCauseInstanceOf(SQLException.class);

        assertThat(contar("voto_procesado")).isZero();
        assertThat(contar("resultado_resumen")).isZero();
        assertThat(contar("resultado_conteo")).isZero();
    }

    private void crearTriggerDeFallo(UUID candidato) {
        jdbcTemplate.execute("""
                CREATE OR REPLACE FUNCTION fallo_conteo() RETURNS trigger AS $$
                BEGIN
                    IF NEW.candidato_id = '%s'::uuid THEN
                        RAISE EXCEPTION 'fallo simulado de conteo';
                    END IF;
                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql
                """.formatted(candidato));
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_fallo_conteo ON resultado_conteo");
        jdbcTemplate.execute("CREATE TRIGGER trg_fallo_conteo BEFORE INSERT OR UPDATE ON resultado_conteo "
                + "FOR EACH ROW EXECUTE FUNCTION fallo_conteo()");
    }

    private long contar(String tabla) {
        Long total = jdbcTemplate.queryForObject("SELECT count(*) FROM " + tabla, Long.class);
        return total == null ? 0 : total;
    }
}
