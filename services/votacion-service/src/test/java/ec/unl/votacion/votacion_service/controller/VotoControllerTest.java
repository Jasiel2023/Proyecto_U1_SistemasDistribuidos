package ec.unl.votacion.votacion_service.controller;

import ec.unl.votacion.votacion_service.dto.VotoResponse;
import ec.unl.votacion.votacion_service.exception.GlobalExceptionHandler;
import ec.unl.votacion.votacion_service.exception.VotoDuplicadoException;
import ec.unl.votacion.votacion_service.service.VotoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VotoControllerTest {

    @Mock
    private VotoService votoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new VotoController(votoService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void creaVotoYDevuelve201() throws Exception {
        when(votoService.registrarVoto(any())).thenReturn(new VotoResponse(
                100L, 1L, 2L, 15L, Instant.parse("2026-11-09T20:00:00Z"),
                "Voto registrado correctamente"
        ));

        mockMvc.perform(post("/api/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "votacionId": 1,
                                  "opcionId": 2,
                                  "usuarioId": 15
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.mensaje").value("Voto registrado correctamente"));
    }

    @Test
    void rechazaRequestInvalidoCon400() throws Exception {
        mockMvc.perform(post("/api/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "votacionId": 0,
                                  "opcionId": 2,
                                  "usuarioId": 15
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void transformaVotoDuplicadoEn409() throws Exception {
        when(votoService.registrarVoto(any()))
                .thenThrow(new VotoDuplicadoException(15L, 1L));

        mockMvc.perform(post("/api/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "votacionId": 1,
                                  "opcionId": 2,
                                  "usuarioId": 15
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("VotoDuplicadoException"));
    }
}
