package ec.unl.votacion.autenticacion.messaging.consumer;

import ec.unl.votacion.autenticacion.dto.RegisterRequest;
import ec.unl.votacion.autenticacion.messaging.event.CuentaCreadaEvent;
import ec.unl.votacion.autenticacion.messaging.event.CuentaCrearEvent;
import ec.unl.votacion.autenticacion.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class CuentaCrearConsumer {

    private static final Logger log = LoggerFactory.getLogger(CuentaCrearConsumer.class);

    private final AuthService authService;
    private final RabbitTemplate rabbitTemplate;

    public CuentaCrearConsumer(AuthService authService, RabbitTemplate rabbitTemplate) {
        this.authService = authService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "cuentas.crear")
    public void recibirSolicitudCrearCuenta(CuentaCrearEvent event) {
        log.info("Recibida solicitud de crear cuenta: nickname={}", event.nickname());

        try {
            RegisterRequest request = new RegisterRequest(event.nickname(), event.clave());
            var response = authService.register(request);

            CuentaCreadaEvent eventoRespuesta = new CuentaCreadaEvent(
                    response.getUserId(),
                    event.nickname(),
                    true,
                    "Cuenta creada correctamente"
            );

            rabbitTemplate.convertAndSend("votacion.exchange", "cuenta.creada", eventoRespuesta);
            log.info("Cuenta creada exitosamente: idCuenta={}", response.getUserId());

        } catch (Exception e) {
            log.error("Error al crear cuenta para nickname {}: {}", event.nickname(), e.getMessage());

            CuentaCreadaEvent eventoRespuesta = new CuentaCreadaEvent(
                    null,
                    event.nickname(),
                    false,
                    e.getMessage() != null ? e.getMessage() : "Error desconocido al crear la cuenta"
            );

            rabbitTemplate.convertAndSend("votacion.exchange", "cuenta.creada", eventoRespuesta);
        }
    }
}
