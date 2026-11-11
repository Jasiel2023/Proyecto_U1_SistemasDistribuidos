package ec.unl.votacion.usuarios.messaging.publisher;

import ec.unl.votacion.usuarios.messaging.event.CuentaCrearEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CuentaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CuentaEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${usuarios.exchange.name:votacion.exchange}")
    private String exchangeName;

    @Value("${usuarios.queue.crear.name:cuentas.crear}")
    private String queueCrearName;

    @Value("${usuarios.routing.crear.key:cuenta.crear}")
    private String routingCrearKey;

    public CuentaEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarSolicitudCrearCuenta(CuentaCrearEvent event) {
        log.info("Publicando solicitud de crear cuenta: {}", event.nickname());
        rabbitTemplate.convertAndSend(exchangeName, routingCrearKey, event);
    }
}
