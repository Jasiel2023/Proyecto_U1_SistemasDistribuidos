package ec.unl.votacion.autenticacion.config;

import ec.unl.votacion.autenticacion.entity.EstadoToken;
import ec.unl.votacion.autenticacion.entity.Sesion;
import ec.unl.votacion.autenticacion.repository.SesionRepository;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Filtro que autentica peticiones protegidas a partir del Bearer token.
 * Si el token no es valido simplemente continua la cadena sin autenticar;
 * los endpoints protegidos responderan 401 mediante el AuthenticationEntryPoint.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProperties jwtProperties;
    private final SesionRepository sesionRepository;

    public JwtAuthFilter(JwtProperties jwtProperties, SesionRepository sesionRepository) {
        this.jwtProperties = jwtProperties;
        this.sesionRepository = sesionRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(request, token);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(jwtProperties.signingKey())
                    .build()
                    .parseClaimsJws(token);

            Optional<Sesion> sesionOpt = sesionRepository.findByToken(token);
            if (sesionOpt.isEmpty()) {
                return;
            }

            Sesion sesion = sesionOpt.get();
            boolean activa = sesion.getEstadoToken() == EstadoToken.ACTIVO
                    && sesion.getFechaExpiracion() != null
                    && sesion.getFechaExpiracion().isAfter(LocalDateTime.now())
                    && sesion.getCuenta() != null
                    && sesion.getCuenta().isEstaActiva();

            if (!activa) {
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            sesion.getCuenta().getNickname(),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER")));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ex) {
            log.debug("Token JWT no valido: {}", ex.getMessage());
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}
