package ec.unl.votacion.votacion_service.messaging;

import ec.unl.votacion.votacion_service.config.RabbitConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VotoEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void publicaEventoConExchangeRoutingKeyYMetadatos() {
        VotoEventPublisher publisher = new VotoEventPublisher(rabbitTemplate);
        UUID eventId = UUID.randomUUID();
        VotoEmitidoEvent event = new VotoEmitidoEvent(
                eventId, 1,
                UUID.fromString("00000000-0000-4000-8000-000000000100"),
                UUID.fromString("00000000-0000-4000-8000-000000000001"),
                UUID.fromString("00000000-0000-4000-8000-000000000002"),
                UUID.fromString("00000000-0000-4000-8000-000000000015"),
                OffsetDateTime.parse("2026-11-09T20:00:00Z"));

        publisher.publicar(event);

        ArgumentCaptor<MessagePostProcessor> processorCaptor =
                ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.EXCHANGE),
                eq(RabbitConfig.ROUTING_KEY),
                eq(event),
                processorCaptor.capture()
        );

        Message message = new Message(new byte[0]);
        Message processedMessage = processorCaptor.getValue().postProcessMessage(message);
        assertThat(processedMessage.getMessageProperties().getDeliveryMode())
                .isEqualTo(org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT);
        assertThat((String) processedMessage.getMessageProperties().getHeader("eventId"))
                .isEqualTo(eventId.toString());
    }
}
