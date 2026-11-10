package ec.unl.votacion.votacion_service.messaging;

import ec.unl.votacion.votacion_service.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class VotoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(VotoEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public VotoEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(VotoEmitidoEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE,
                    RabbitConfig.ROUTING_KEY,
                    event,
                    mensajePersistente(event)
            );
        } catch (AmqpException exception) {
            log.error("No fue posible publicar VotoEmitidoEvent {}", event.eventoId(), exception);
        }
    }

    private MessagePostProcessor mensajePersistente(VotoEmitidoEvent event) {
        return message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            message.getMessageProperties().setHeader("eventId", event.eventoId().toString());
            return message;
        };
    }
}
