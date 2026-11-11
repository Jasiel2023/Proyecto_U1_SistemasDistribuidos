package ec.unl.votacion.usuarios.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue queueCrear() {
        return new Queue("cuentas.crear", true);
    }

    @Bean
    public Queue queueCreada() {
        return new Queue("cuentas.creadas", true);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange("votacion.exchange", true, false);
    }

    @Bean
    public Binding bindingCrear(Queue queueCrear, TopicExchange exchange) {
        return BindingBuilder.bind(queueCrear).to(exchange).with("cuenta.crear");
    }

    @Bean
    public Binding bindingCreada(Queue queueCreada, TopicExchange exchange) {
        return BindingBuilder.bind(queueCreada).to(exchange).with("cuenta.creada");
    }
}
