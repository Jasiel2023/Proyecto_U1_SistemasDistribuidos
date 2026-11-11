package ec.unl.votacion.resultados;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import ec.unl.votacion.resultados.application.ResultadoQueryService;
import ec.unl.votacion.resultados.config.ResultadosProperties;
import ec.unl.votacion.resultados.messaging.VotoEmitidoEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

class RabbitMessagingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private ResultadoQueryService queryService;

    @Autowired
    private ResultadosProperties properties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final JsonMapper mapper = JsonMapper.builder().build();

    @BeforeEach
    void limpiar() {
        jdbcTemplate.execute("TRUNCATE TABLE evento_conflicto, voto_procesado, "
                + "resultado_conteo, resultado_resumen RESTART IDENTITY CASCADE");
        amqpAdmin.purgeQueue(properties.messaging().queue(), true);
        amqpAdmin.purgeQueue(properties.messaging().dlq(), true);
    }

    @AfterEach
    void limpiarTrigger() {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_fallo_conteo ON resultado_conteo");
        jdbcTemplate.execute("DROP FUNCTION IF EXISTS fallo_conteo()");
    }

    @Test
    void eventoValidoSePersisteAntesDeConfirmar() {
        UUID eleccion = UUID.randomUUID();
        UUID candidato = UUID.randomUUID();

        publicar(TestEventos.voto(eleccion, candidato));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(queryService.obtener(eleccion).totalVotos()).isEqualTo(1));
        assertThat(queryService.obtener(eleccion).conteos().getFirst().candidatoId())
                .isEqualTo(candidato);
    }

    @Test
    void reentregaDelMismoMensajeNoIncrementa() {
        UUID eleccion = UUID.randomUUID();
        VotoEmitidoEvent evento = TestEventos.voto(eleccion, UUID.randomUUID());

        publicar(evento);
        publicar(evento);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(queryService.obtener(eleccion).totalVotos()).isEqualTo(1));
        sleep(700);
        assertThat(queryService.obtener(eleccion).totalVotos()).isEqualTo(1);
        assertThat(contar("voto_procesado")).isEqualTo(1);
    }

    @Test
    void mensajeInvalidoTerminaEnDlq() {
        publicarCrudo("{ esto no es json valido");

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(recibirDlq()).isNotNull());
        assertThat(contar("voto_procesado")).isZero();
    }

    @Test
    void rollbackPorFalloDeBaseVaADlqSinEstadoParcial() {
        UUID eleccion = UUID.randomUUID();
        UUID candidato = UUID.randomUUID();
        crearTriggerDeFallo(candidato);

        publicar(TestEventos.voto(eleccion, candidato));

        await().atMost(Duration.ofSeconds(40)).untilAsserted(() ->
                assertThat(recibirDlq()).isNotNull());
        assertThat(contar("voto_procesado")).isZero();
        assertThat(contar("resultado_resumen")).isZero();
        assertThat(contar("resultado_conteo")).isZero();
    }

    @Test
    void consumidoresConcurrentesMantienenConteosExactos() throws Exception {
        UUID eleccion = UUID.randomUUID();
        UUID candidato = UUID.randomUUID();
        int unicos = 30;
        List<VotoEmitidoEvent> eventos = new ArrayList<>();
        for (int i = 0; i < unicos; i++) {
            eventos.add(TestEventos.voto(eleccion, candidato));
        }

        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch salida = new CountDownLatch(1);
        try {
            for (VotoEmitidoEvent evento : eventos) {
                for (int repeticion = 0; repeticion < 2; repeticion++) {
                    pool.submit(() -> {
                        salida.await();
                        publicar(evento);
                        return null;
                    });
                }
            }
            salida.countDown();
            pool.shutdown();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        await().atMost(Duration.ofSeconds(40)).untilAsserted(() ->
                assertThat(queryService.obtener(eleccion).totalVotos()).isEqualTo(unicos));
        sleep(1000);
        assertThat(queryService.obtener(eleccion).totalVotos()).isEqualTo(unicos);
        assertThat(contar("voto_procesado")).isEqualTo(unicos);
    }

    @Test
    void aislaEleccionesEnConsumo() {
        UUID eleccionA = UUID.randomUUID();
        UUID eleccionB = UUID.randomUUID();
        UUID candidato = UUID.randomUUID();

        publicar(TestEventos.voto(eleccionA, candidato));
        publicar(TestEventos.voto(eleccionB, candidato));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(queryService.obtener(eleccionA).totalVotos()).isEqualTo(1);
            assertThat(queryService.obtener(eleccionB).totalVotos()).isEqualTo(1);
        });
    }

    private void publicar(VotoEmitidoEvent evento) {
        try {
            publicarCrudo(mapper.writeValueAsString(evento));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void publicarCrudo(String payload) {
        Message mensaje = MessageBuilder.withBody(payload.getBytes(UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(UTF_8.name())
                .setDeliveryMode(MessageProperties.DEFAULT_DELIVERY_MODE)
                .build();
        rabbitTemplate.send(properties.messaging().exchange(),
                properties.messaging().routingKey(), mensaje);
    }

    private Message recibirDlq() {
        return rabbitTemplate.receive(properties.messaging().dlq(), 1000);
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

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
