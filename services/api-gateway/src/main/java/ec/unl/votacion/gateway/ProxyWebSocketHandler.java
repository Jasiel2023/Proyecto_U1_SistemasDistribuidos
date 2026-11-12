package ec.unl.votacion.gateway;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

class ProxyWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ProxyWebSocketHandler.class);
    private static final long TIMEOUT_SEGUNDOS = 10;

    private final URI backendUri;
    private final WebSocketClient client = new StandardWebSocketClient();
    private final Map<String, WebSocketSession> backends = new ConcurrentHashMap<>();

    ProxyWebSocketHandler(URI backendUri) {
        this.backendUri = backendUri;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession cliente) throws Exception {
        try {
            WebSocketSession backend = client.execute(new RelayHandler(cliente),
                            new WebSocketHttpHeaders(), backendUri)
                    .get(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
            backends.put(cliente.getId(), backend);
        } catch (Exception ex) {
            log.warn("No se pudo conectar al WebSocket de Resultados en {}", backendUri, ex);
            cerrar(cliente, CloseStatus.SERVER_ERROR);
            throw ex;
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession cliente, TextMessage mensaje) throws Exception {
        reenviar(backends.get(cliente.getId()), new TextMessage(mensaje.getPayload()));
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession cliente, BinaryMessage mensaje) throws Exception {
        reenviar(backends.get(cliente.getId()), new BinaryMessage(mensaje.getPayload()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession cliente, CloseStatus estado) {
        cerrar(backends.remove(cliente.getId()), estado);
    }

    @Override
    public void handleTransportError(WebSocketSession cliente, Throwable error) {
        log.debug("Error de transporte WebSocket en cliente {}", cliente.getId(), error);
        cerrar(backends.remove(cliente.getId()), CloseStatus.SERVER_ERROR);
        cerrar(cliente, CloseStatus.SERVER_ERROR);
    }

    private void reenviar(WebSocketSession destino, org.springframework.web.socket.WebSocketMessage<?> mensaje)
            throws IOException {
        if (destino == null || !destino.isOpen()) {
            return;
        }
        synchronized (destino) {
            if (destino.isOpen()) {
                destino.sendMessage(mensaje);
            }
        }
    }

    private void cerrar(WebSocketSession sesion, CloseStatus estado) {
        if (sesion == null || !sesion.isOpen()) {
            return;
        }
        try {
            sesion.close(estado);
        } catch (IOException ex) {
            log.debug("No se pudo cerrar la sesión WebSocket {}", sesion.getId(), ex);
        }
    }

    private final class RelayHandler extends AbstractWebSocketHandler {

        private final WebSocketSession cliente;

        private RelayHandler(WebSocketSession cliente) {
            this.cliente = cliente;
        }

        @Override
        protected void handleTextMessage(WebSocketSession backend, TextMessage mensaje) throws Exception {
            reenviar(cliente, new TextMessage(mensaje.getPayload()));
        }

        @Override
        protected void handleBinaryMessage(WebSocketSession backend, BinaryMessage mensaje) throws Exception {
            reenviar(cliente, new BinaryMessage(mensaje.getPayload()));
        }

        @Override
        public void afterConnectionClosed(WebSocketSession backend, CloseStatus estado) {
            cerrar(cliente, estado);
        }

        @Override
        public void handleTransportError(WebSocketSession backend, Throwable error) {
            log.debug("Error de transporte WebSocket en backend {}", backend.getId(), error);
            cerrar(cliente, CloseStatus.SERVER_ERROR);
        }
    }
}
