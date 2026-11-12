package ec.unl.votacion.votacion_service.controller;

import ec.unl.votacion.votacion_service.dto.VotoResponse;
import ec.unl.votacion.votacion_service.exception.GlobalExceptionHandler;
import ec.unl.votacion.votacion_service.exception.VotoDuplicadoException;
import ec.unl.votacion.votacion_service.service.VotoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VotoControllerTest {

    private static final UUID VOTO_ID = UUID.fromString("00000000-0000-4000-8000-000000000100");
    private static final UUID ELECCION_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID CANDIDATO_ID = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private static final UUID USUARIO_ID = UUID.fromString("00000000-0000-4000-8000-000000000015");

    @Mock
    private VotoService votoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new VotoController(votoService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private void autenticarComo(UUID idUsuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(idUsuario.toString(), null, List.of()));
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void creaVotoYDevuelve201() throws Exception {
        autenticarComo(USUARIO_ID);
        when(votoService.registrarVoto(any(), any())).thenReturn(new VotoResponse(
                VOTO_ID, ELECCION_ID, CANDIDATO_ID, USUARIO_ID, Instant.parse("2026-11-09T20:00:00Z"),
                "Voto registrado correctamente"
        ));

        mockMvc.perform(post("/api/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eleccionId": "00000000-0000-4000-8000-000000000001",
                                  "candidatoId": "00000000-0000-4000-8000-000000000002"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(VOTO_ID.toString()))
                .andExpect(jsonPath("$.mensaje").value("Voto registrado correctamente"));
    }

    @Test
    void rechazaRequestInvalidoCon400() throws Exception {
        autenticarComo(USUARIO_ID);
        mockMvc.perform(post("/api/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eleccionId": 0,
                                  "candidatoId": 2
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    }

    @Test
    void transformaVotoDuplicadoEn409() throws Exception {
        autenticarComo(USUARIO_ID);
        when(votoService.registrarVoto(any(), any()))
                .thenThrow(new VotoDuplicadoException(USUARIO_ID, ELECCION_ID));

        mockMvc.perform(post("/api/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eleccionId": "00000000-0000-4000-8000-000000000001",
                                  "candidatoId": "00000000-0000-4000-8000-000000000002"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("VotoDuplicadoException"));
    }
}
