package ec.unl.votacion.resultados.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "resultados")
public record ResultadosProperties(Messaging messaging, Websocket websocket, Cors cors) {

    public record Messaging(
            String exchange,
            String routingKey,
            String queue,
            String dlx,
            String dlq,
            String dlRoutingKey) {
    }

    public record Websocket(
            String endpoint,
            String topicPrefix,
            List<String> allowedOrigins) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
