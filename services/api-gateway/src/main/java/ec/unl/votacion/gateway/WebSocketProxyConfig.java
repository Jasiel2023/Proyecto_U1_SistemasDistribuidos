package ec.unl.votacion.gateway;

import java.net.URI;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketProxyConfig implements WebSocketConfigurer {

    private final String resultadosWsUrl;
    private final String allowedOrigins;

    public WebSocketProxyConfig(
            @Value("${RESULTADOS_WS_URL:ws://localhost:8083/ws}") String resultadosWsUrl,
            @Value("${GATEWAY_WS_ALLOWED_ORIGINS:http://localhost:5173,http://localhost:3000}") String allowedOrigins) {
        this.resultadosWsUrl = resultadosWsUrl;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origenes = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .toArray(String[]::new);
        registry.addHandler(new ProxyWebSocketHandler(URI.create(resultadosWsUrl)), "/ws")
                .setAllowedOriginPatterns(origenes);
    }
}
