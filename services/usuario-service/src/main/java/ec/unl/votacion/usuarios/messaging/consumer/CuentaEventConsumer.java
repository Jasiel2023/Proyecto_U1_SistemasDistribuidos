package ec.unl.votacion.usuarios.messaging.consumer;

import ec.unl.votacion.usuarios.messaging.event.CuentaCreadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CuentaEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(CuentaEventConsumer.class);

    @Value("${usuarios.queue.creada.name:cuentas.creadas}")
    private String queueCreadaName;

    @Value("${usuarios.routing.creada.key:cuenta.creada}")
    private String routingCreadaKey;

    private final ConcurrentHashMap<String, CompletableFuture<CuentaCreadaEvent>> pendientes = new ConcurrentHashMap<>();

    public void registrarPendiente(String nickname, CompletableFuture<CuentaCreadaEvent> futuro) {
        pendientes.put(nickname, futuro);
    }

    @RabbitListener(queues = "${usuarios.queue.creada.name:cuentas.creadas}")
    public void recibirRespuestaCrearCuenta(CuentaCreadaEvent event) {
        log.info("Recibida respuesta de cuenta creada: idCuenta={}, exito={}", event.idCuenta(), event.exito());

        CompletableFuture<CuentaCreadaEvent> futuro = pendientes.remove(event.nickname());
        if (futuro != null) {
            futuro.complete(event);
        } else {
            log.warn("No se encontro futuro pendiente para el nickname: {}", event.nickname());
        }
    }

    public CompletableFuture<CuentaCreadaEvent> obtenerFuturo(String nickname) {
        return pendientes.get(nickname);
    }
}
