package ec.unl.votacion.resultados.messaging;

import ec.unl.votacion.resultados.application.ResultadoCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class VotoEmitidoListener {

    private static final Logger log = LoggerFactory.getLogger(VotoEmitidoListener.class);

    private final ResultadoCommandService commandService;

    public VotoEmitidoListener(ResultadoCommandService commandService) {
        this.commandService = commandService;
    }

    @RabbitListener(queues = "${resultados.messaging.queue}")
    public void recibir(VotoEmitidoEvent evento) {
        log.info("Evento voto.emitido recibido eventId={} votoId={} eleccionId={} candidatoId={}",
                evento.eventId(), evento.votoId(), evento.eleccionId(), evento.candidatoId());
        commandService.procesar(evento);
    }
}
