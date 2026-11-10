package ec.unl.votacion.votacion_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "votacion.exchange";
    public static final String QUEUE = "votos.procesados";
    public static final String ROUTING_KEY = "voto.emitido";

    @Bean
    TopicExchange votacionExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue votosProcesadosQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    Binding votosProcesadosBinding(Queue votosProcesadosQueue, TopicExchange votacionExchange) {
        return BindingBuilder.bind(votosProcesadosQueue)
                .to(votacionExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
