package ec.unl.votacion.resultados.messaging;

import ec.unl.votacion.resultados.application.ResultadoCommandService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class VotoEmitidoListener {

    private final ResultadoCommandService commandService;

    public VotoEmitidoListener(ResultadoCommandService commandService) {
        this.commandService = commandService;
    }

    @RabbitListener(queues = "${resultados.messaging.queue}")
    public void recibir(VotoEmitidoEvent evento) {
        commandService.procesar(evento);
    }
}
