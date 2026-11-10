package ec.unl.votacion.resultados.messaging;

import ec.unl.votacion.resultados.config.ResultadosProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfig {

    @Bean
    public Declarables resultadosTopology(ResultadosProperties properties) {
        ResultadosProperties.Messaging m = properties.messaging();

        TopicExchange exchange = new TopicExchange(m.exchange(), true, false);
        TopicExchange deadLetterExchange = new TopicExchange(m.dlx(), true, false);

        Queue queue = QueueBuilder.durable(m.queue())
                .deadLetterExchange(m.dlx())
                .deadLetterRoutingKey(m.dlRoutingKey())
                .build();
        Queue deadLetterQueue = QueueBuilder.durable(m.dlq()).build();

        Binding binding = BindingBuilder.bind(queue).to(exchange).with(m.routingKey());
        Binding deadLetterBinding = BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange).with(m.dlRoutingKey());

        return new Declarables(exchange, deadLetterExchange, queue, deadLetterQueue,
                binding, deadLetterBinding);
    }

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}
