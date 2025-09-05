package com.peluqueria.recepcionista_virtual.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    @Autowired
    private JwtUserDetailsService jwtUserDetailsService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String requestPath = request.getServletPath();
        String method = request.getMethod();

        // Log para debug
        logger.debug("JWT Filter - Path: {}, Method: {}", requestPath, method);

        // CRÍTICO: Saltar validación para rutas públicas y OPTIONS
        if ("OPTIONS".equalsIgnoreCase(method) ||
                requestPath.equals("/api/auth/login") ||
                requestPath.equals("/api/auth/register") ||
                requestPath.startsWith("/api/twilio/") ||
                requestPath.equals("/health") ||
                requestPath.startsWith("/ws") ||
                requestPath.startsWith("/actuator/")) {

            logger.debug("Ruta pública o OPTIONS - saltando JWT");
            chain.doFilter(request, response);
            return;
        }

        final String requestTokenHeader = request.getHeader("Authorization");

        String username = null;
        String jwtToken = null;
        String tenantId = null;

        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                username = jwtTokenUtil.extractUsername(jwtToken);
                tenantId = jwtTokenUtil.extractTenantId(jwtToken);

                logger.debug("TenantId extraído del JWT: {}", tenantId);

            } catch (Exception e) {
                logger.error("Error procesando JWT: ", e);
            }
        }

        // Validar token y configurar autenticación
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.jwtUserDetailsService.loadUserByUsername(username);

            if (jwtTokenUtil.validateToken(jwtToken, userDetails.getUsername())) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                // CORRECCIÓN: Establecer tenantId DESPUÉS de validación exitosa
                if (tenantId != null) {
                    request.setAttribute("tenantId", tenantId);
                    logger.debug("Usuario autenticado: {} para tenant: {} - tenantId establecido en request", username, tenantId);
                } else {
                    logger.warn("Usuario autenticado pero tenantId es null: {}", username);
                }
            } else {
                logger.warn("Token inválido para usuario: {}", username);
            }
        } else {
            // IMPORTANTE: También establecer tenantId para rutas que no requieren autenticación completa
            if (tenantId != null) {
                request.setAttribute("tenantId", tenantId);
                logger.debug("TenantId establecido en request sin autenticación completa: {}", tenantId);
            }
        }

        chain.doFilter(request, response);
    }
}