package ec.unl.votacion.votacion_service.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "votacion.exchange";
    public static final String ROUTING_KEY = "voto.emitido";

    @Bean
    TopicExchange votacionExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
