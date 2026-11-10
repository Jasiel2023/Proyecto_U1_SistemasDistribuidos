package ec.unl.votacion.votacion_service.service;

import ec.unl.votacion.votacion_service.dto.CrearVotoRequest;
import ec.unl.votacion.votacion_service.dto.VotoResponse;
import ec.unl.votacion.votacion_service.entity.Opcion;
import ec.unl.votacion.votacion_service.entity.Votacion;
import ec.unl.votacion.votacion_service.entity.Voto;
import ec.unl.votacion.votacion_service.exception.OpcionNoEncontradaException;
import ec.unl.votacion.votacion_service.exception.OpcionInactivaException;
import ec.unl.votacion.votacion_service.exception.VotacionCerradaException;
import ec.unl.votacion.votacion_service.exception.VotoDuplicadoException;
import ec.unl.votacion.votacion_service.messaging.VotoEmitidoEvent;
import ec.unl.votacion.votacion_service.repository.OpcionRepository;
import ec.unl.votacion.votacion_service.repository.VotacionRepository;
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
    private VotacionRepository votacionRepository;

    @Mock
    private OpcionRepository opcionRepository;

    @Mock
    private VotoRepository votoRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Votacion votacion;

    @Mock
    private Opcion opcion;

    @InjectMocks
    private VotoService votoService;

    @Test
    void registraUnVotoValido() {
        CrearVotoRequest request = new CrearVotoRequest(1L, 2L, 15L);
        when(votacionRepository.findById(1L)).thenReturn(Optional.of(votacion));
        when(votacion.estaAbierta()).thenReturn(true);
        when(votacion.getId()).thenReturn(1L);
        when(opcionRepository.findByIdAndVotacionId(2L, 1L))
                .thenReturn(Optional.of(opcion));
        when(opcion.estaActiva()).thenReturn(true);
        when(opcion.getId()).thenReturn(2L);
        when(votoRepository.existsByVotacionIdAndUsuarioId(1L, 15L))
                .thenReturn(false);
        when(votoRepository.save(any(Voto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        VotoResponse response = votoService.registrarVoto(request);

        assertThat(response.votacionId()).isEqualTo(1L);
        assertThat(response.opcionId()).isEqualTo(2L);
        assertThat(response.usuarioId()).isEqualTo(15L);

        ArgumentCaptor<Voto> votoCaptor = ArgumentCaptor.forClass(Voto.class);
        verify(votoRepository).save(votoCaptor.capture());
        assertThat(votoCaptor.getValue().getUsuarioId()).isEqualTo(15L);
        verify(eventPublisher).publishEvent(any(VotoEmitidoEvent.class));
    }

    @Test
    void rechazaVotoCuandoLaVotacionEstaCerrada() {
        CrearVotoRequest request = new CrearVotoRequest(1L, 2L, 15L);
        when(votacionRepository.findById(1L)).thenReturn(Optional.of(votacion));
        when(votacion.estaAbierta()).thenReturn(false);
        when(votacion.getId()).thenReturn(1L);

        assertThatThrownBy(() -> votoService.registrarVoto(request))
                .isInstanceOf(VotacionCerradaException.class);

        verify(opcionRepository, never()).findByIdAndVotacionId(any(), any());
        verify(votoRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rechazaOpcionQueNoPerteneceALaVotacion() {
        CrearVotoRequest request = new CrearVotoRequest(1L, 2L, 15L);
        when(votacionRepository.findById(1L)).thenReturn(Optional.of(votacion));
        when(votacion.estaAbierta()).thenReturn(true);
        when(opcionRepository.findByIdAndVotacionId(2L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> votoService.registrarVoto(request))
                .isInstanceOf(OpcionNoEncontradaException.class);

        verify(votoRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rechazaVotoDuplicadoAntesDePersistir() {
        CrearVotoRequest request = new CrearVotoRequest(1L, 2L, 15L);
        when(votacionRepository.findById(1L)).thenReturn(Optional.of(votacion));
        when(votacion.estaAbierta()).thenReturn(true);
        when(opcionRepository.findByIdAndVotacionId(2L, 1L))
                .thenReturn(Optional.of(opcion));
        when(opcion.estaActiva()).thenReturn(true);
        when(votoRepository.existsByVotacionIdAndUsuarioId(1L, 15L))
                .thenReturn(true);

        assertThatThrownBy(() -> votoService.registrarVoto(request))
                .isInstanceOf(VotoDuplicadoException.class);

        verify(votoRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rechazaOpcionInactiva() {
        CrearVotoRequest request = new CrearVotoRequest(1L, 2L, 15L);
        when(votacionRepository.findById(1L)).thenReturn(Optional.of(votacion));
        when(votacion.estaAbierta()).thenReturn(true);
        when(opcionRepository.findByIdAndVotacionId(2L, 1L))
                .thenReturn(Optional.of(opcion));
        when(opcion.estaActiva()).thenReturn(false);
        when(opcion.getId()).thenReturn(2L);

        assertThatThrownBy(() -> votoService.registrarVoto(request))
                .isInstanceOf(OpcionInactivaException.class);

        verify(votoRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void noPublicaEventoCuandoFallaLaPersistencia() {
        CrearVotoRequest request = new CrearVotoRequest(1L, 2L, 15L);
        when(votacionRepository.findById(1L)).thenReturn(Optional.of(votacion));
        when(votacion.estaAbierta()).thenReturn(true);
        when(opcionRepository.findByIdAndVotacionId(2L, 1L))
                .thenReturn(Optional.of(opcion));
        when(opcion.estaActiva()).thenReturn(true);
        when(votoRepository.existsByVotacionIdAndUsuarioId(1L, 15L))
                .thenReturn(false);
        when(votoRepository.save(any(Voto.class)))
                .thenThrow(new DataIntegrityViolationException("fallo de persistencia"));

        assertThatThrownBy(() -> votoService.registrarVoto(request))
                .isInstanceOf(VotoDuplicadoException.class);

        verify(eventPublisher, never()).publishEvent(any());
    }
}
