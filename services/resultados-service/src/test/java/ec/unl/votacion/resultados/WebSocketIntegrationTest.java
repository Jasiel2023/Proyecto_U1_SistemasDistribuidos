package ec.unl.votacion.resultados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.unl.votacion.resultados.application.ResultadoCommandService;
import ec.unl.votacion.resultados.application.ResultadoSnapshot;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import tools.jackson.databind.json.JsonMapper;

class WebSocketIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int puerto;

    @Autowired
    private ResultadoCommandService commandService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final JsonMapper mapper = JsonMapper.builder().build();

    private WebSocketStompClient cliente;
    private StompSession sesion;

    @BeforeEach
    void limpiar() {
        jdbcTemplate.execute("TRUNCATE TABLE evento_conflicto, voto_procesado, "
                + "resultado_conteo, resultado_resumen RESTART IDENTITY CASCADE");
        cliente = new WebSocketStompClient(new StandardWebSocketClient());
        cliente.setMessageConverter(new JacksonJsonMessageConverter());
    }

    @AfterEach
    void cerrar() {
        if (sesion != null && sesion.isConnected()) {
            sesion.disconnect();
        }
        if (cliente != null) {
            cliente.stop();
        }
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_fallo_conteo ON resultado_conteo");
        jdbcTemplate.execute("DROP FUNCTION IF EXISTS fallo_conteo()");
    }

    @Test
    void notificaSnapshotDespuesDelCommit() throws Exception {
        UUID eleccion = UUID.randomUUID();
        BlockingQueue<ResultadoSnapshot> recibidos = suscribir(eleccion);

        commandService.procesar(TestEventos.voto(eleccion, UUID.randomUUID()));

        ResultadoSnapshot snapshot = recibidos.poll(10, TimeUnit.SECONDS);
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.eleccionId()).isEqualTo(eleccion);
        assertThat(snapshot.totalVotos()).isEqualTo(1);
        assertThat(snapshot.revision()).isEqualTo(1);
        assertThat(snapshot.disponible()).isTrue();
    }

    @Test
    void noNotificaSiLaTransaccionSeRevierte() throws Exception {
        UUID eleccion = UUID.randomUUID();
        UUID candidato = UUID.randomUUID();
        BlockingQueue<ResultadoSnapshot> recibidos = suscribir(eleccion);
        crearTriggerDeFallo(candidato);

        assertThatThrownBy(() -> commandService.procesar(TestEventos.voto(eleccion, candidato)))
                .hasRootCauseInstanceOf(SQLException.class);

        assertThat(recibidos.poll(2500, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void aislaDestinosPorEleccion() throws Exception {
        UUID eleccionA = UUID.randomUUID();
        UUID eleccionB = UUID.randomUUID();
        BlockingQueue<ResultadoSnapshot> recibidosA = suscribir(eleccionA);
        BlockingQueue<ResultadoSnapshot> recibidosB = suscribirEnOtraSesion(eleccionB);

        commandService.procesar(TestEventos.voto(eleccionA, UUID.randomUUID()));

        ResultadoSnapshot snapshotA = recibidosA.poll(10, TimeUnit.SECONDS);
        assertThat(snapshotA).isNotNull();
        assertThat(snapshotA.eleccionId()).isEqualTo(eleccionA);
        assertThat(recibidosB.poll(1500, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void restRecuperaLaRevisionNotificada() throws Exception {
        UUID eleccion = UUID.randomUUID();
        BlockingQueue<ResultadoSnapshot> recibidos = suscribir(eleccion);

        commandService.procesar(TestEventos.voto(eleccion, UUID.randomUUID()));

        ResultadoSnapshot notificado = recibidos.poll(10, TimeUnit.SECONDS);
        assertThat(notificado).isNotNull();
        HttpRequest request = HttpRequest.newBuilder(URI.create(
                        "http://localhost:" + puerto + "/api/resultados/elecciones/" + eleccion))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();
        HttpResponse<String> respuesta = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        ResultadoSnapshot consultado = mapper.readValue(respuesta.body(), ResultadoSnapshot.class);
        assertThat(consultado.revision()).isEqualTo(notificado.revision());
        assertThat(consultado.totalVotos()).isEqualTo(notificado.totalVotos());
    }

    private BlockingQueue<ResultadoSnapshot> suscribir(UUID eleccion) throws Exception {
        sesion = conectar();
        return suscribir(sesion, eleccion);
    }

    private BlockingQueue<ResultadoSnapshot> suscribirEnOtraSesion(UUID eleccion) throws Exception {
        StompSession otra = conectar();
        return suscribir(otra, eleccion);
    }

    private StompSession conectar() throws Exception {
        StompSession nueva = cliente.connectAsync(
                        "ws://localhost:" + puerto + "/ws", new StompSessionHandlerAdapter() {
                        })
                .get(10, TimeUnit.SECONDS);
        return nueva;
    }

    private BlockingQueue<ResultadoSnapshot> suscribir(StompSession session, UUID eleccion) throws Exception {
        BlockingQueue<ResultadoSnapshot> recibidos = new LinkedBlockingQueue<>();
        session.subscribe("/topic/resultados/elecciones/" + eleccion, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ResultadoSnapshot.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                recibidos.add((ResultadoSnapshot) payload);
            }
        });
        Thread.sleep(300);
        return recibidos;
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
}

