package ec.unl.votacion.votacion_service.service;

import ec.unl.votacion.votacion_service.dto.CrearVotoRequest;
import ec.unl.votacion.votacion_service.dto.VotoResponse;
import ec.unl.votacion.votacion_service.entity.Candidato;
import ec.unl.votacion.votacion_service.entity.Eleccion;
import ec.unl.votacion.votacion_service.entity.Voto;
import ec.unl.votacion.votacion_service.exception.CandidatoNoEncontradoException;
import ec.unl.votacion.votacion_service.exception.CandidatoInactivoException;
import ec.unl.votacion.votacion_service.exception.EleccionCerradaException;
import ec.unl.votacion.votacion_service.exception.VotoDuplicadoException;
import ec.unl.votacion.votacion_service.messaging.VotoEmitidoEvent;
import ec.unl.votacion.votacion_service.repository.CandidatoRepository;
import ec.unl.votacion.votacion_service.repository.EleccionRepository;
import ec.unl.votacion.votacion_service.repository.VotoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VotoServiceTest {

    @Mock
    private EleccionRepository eleccionRepository;

    @Mock
    private CandidatoRepository candidatoRepository;

    @Mock
    private VotoRepository votoRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Eleccion eleccion;

    @Mock
    private Candidato candidato;

    @InjectMocks
    private VotoService votoService;

    @Test
    void registraUnVotoValido() {
        CrearVotoRequest request = new CrearVotoRequest(1L, 2L, 15L);
        when(eleccionRepository.findById(1L)).thenReturn(Optional.of(eleccion));
        when(eleccion.estaActiva()).thenReturn(true);
        when(eleccion.getId()).thenReturn(1L);
        when(candidatoRepository.findByIdAndEleccionId(2L, 1L))
                .thenReturn(Optional.of(candidato));
        when(candidato.estaActivo()).thenReturn(true);
        when(candidato.getId()).thenReturn(2L);
        when(votoRepository.existsByEleccionIdAndIdUsuario(1L, 15L))
                .thenReturn(false);
        when(votoRepository.save(any(Voto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VotoResponse response = votoService.registrarVoto(request);

        assertThat(response.eleccionId()).isEqualTo(1L);
        assertThat(response.candidatoId()).isEqualTo(2L);
        assertThat(response.idUsuario()).isEqualTo(15L);

        ArgumentCaptor<Voto> votoCaptor = ArgumentCaptor.forClass(Voto.class);
        verify(votoRepository).save(votoCaptor.capture());
        assertThat(votoCaptor.getValue().getIdUsuario()).isEqualTo(15L);
        verify(eventPublisher).publishEvent(any(VotoEmitidoEvent.class));
    }

    @Test
    void rechazaVotoCuandoLaEleccionEstaInactiva() {
        CrearVotoRequest request = new CrearVotoRequest(1L, 2L, 15L);
        when(eleccionRepository.findById(1L)).thenReturn(Optional.of(eleccion));
        when(eleccion.estaActiva()).thenReturn(false);
        when(eleccion.getId()).thenReturn(1L);

        assertThatThrownBy(() -> votoService.registrarVoto(request))
                .isInstanceOf(EleccionCerradaException.class);

        verify(candidatoRepository, never()).findByIdAndEleccionId(any(), any());
        verify(votoRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rechazaCandidatoQueNoPerteneceALaEleccion() {
        CrearVotoRequest request = new CrearVotoRequest(1L, 2L, 15L);
        when(eleccionRepository.findById(1L)).thenReturn(Optional.of(eleccion));
        when(eleccion.estaActiva()).thenReturn(true);
        when(candidatoRepository.findByIdAndEleccionId(2L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> votoService.registrarVoto(request))
                .isInstanceOf(CandidatoNoEncontradoException.class);

        verify(votoRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rechazaVotoDuplicadoAntesDePersistir() {
        CrearVotoRequest request = new CrearVotoRequest(1L, 2L, 15L);
        when(eleccionRepository.findById(1L)).thenReturn(Optional.of(eleccion));
        when(eleccion.estaActiva()).thenReturn(true);
        when(candidatoRepository.findByIdAndEleccionId(2L, 1L))
                .thenReturn(Optional.of(candidato));
        when(candidato.estaActivo()).thenReturn(true);
        when(votoRepository.existsByEleccionIdAndIdUsuario(1L, 15L))
                .thenReturn(true);

        assertThatThrownBy(() -> votoService.registrarVoto(request))
                .isInstanceOf(VotoDuplicadoException.class);

        verify(votoRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rechazaCandidatoInactivo() {
        CrearVotoRequest request = new CrearVotoRequest(1L, 2L, 15L);
        when(eleccionRepository.findById(1L)).thenReturn(Optional.of(eleccion));
        when(eleccion.estaActiva()).thenReturn(true);
        when(candidatoRepository.findByIdAndEleccionId(2L, 1L))
                .thenReturn(Optional.of(candidato));
        when(candidato.estaActivo()).thenReturn(false);
        when(candidato.getId()).thenReturn(2L);

        assertThatThrownBy(() -> votoService.registrarVoto(request))
                .isInstanceOf(CandidatoInactivoException.class);

        verify(votoRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void noPublicaEventoCuandoFallaLaPersistencia() {
        CrearVotoRequest request = new CrearVotoRequest(1L, 2L, 15L);
        when(eleccionRepository.findById(1L)).thenReturn(Optional.of(eleccion));
        when(eleccion.estaActiva()).thenReturn(true);
        when(candidatoRepository.findByIdAndEleccionId(2L, 1L))
                .thenReturn(Optional.of(candidato));
        when(candidato.estaActivo()).thenReturn(true);
        when(votoRepository.existsByEleccionIdAndIdUsuario(1L, 15L))
                .thenReturn(false);
        when(votoRepository.save(any(Voto.class)))
                .thenThrow(new DataIntegrityViolationException("fallo de persistencia"));

        assertThatThrownBy(() -> votoService.registrarVoto(request))
                .isInstanceOf(VotoDuplicadoException.class);

        verify(eventPublisher, never()).publishEvent(any());
    }
}
