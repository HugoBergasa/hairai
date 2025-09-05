package com.peluqueria.recepcionista_virtual.controller;

import com.peluqueria.recepcionista_virtual.dto.*;
import com.peluqueria.recepcionista_virtual.model.*;
import com.peluqueria.recepcionista_virtual.security.JwtTokenUtil;
import com.peluqueria.recepcionista_virtual.service.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Credenciales inválidas", e);
        }

        final User user = userService.findByEmail(request.getEmail());
        final String token = jwtTokenUtil.generateToken(
                user.getEmail(),
                user.getTenant().getId(),
                user.getRole()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", UserDTO.fromUser(user));
        response.put("tenant", TenantDTO.fromTenant(user.getTenant()));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        logger.info("=== INICIO REGISTRO MULTI-TENANT ===");
        logger.info("Request recibido - Email: {}, Nueva peluquería: {}, Nombre: {}",
                request.getEmail(),
                request.isNewTenant(),
                request.getNombrePeluqueria());

        try {
            // Verificar si el email ya existe
            if (userService.existsByEmail(request.getEmail())) {
                logger.warn("Email ya registrado: {}", request.getEmail());
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El email ya está registrado"));
            }

            // MULTI-TENANT: Crear o asignar tenant
            Tenant tenant;
            if (request.isNewTenant()) {
                logger.info("Creando nuevo tenant: {}", request.getNombrePeluqueria());
                tenant = new Tenant();
                tenant.setNombrePeluqueria(request.getNombrePeluqueria());
                tenant.setTelefono(request.getTelefono());
                tenant.setDireccion(request.getDireccion());
                tenant = tenantService.save(tenant);

                // Crear servicios predeterminados para el tenant
                tenantService.createDefaultServices(tenant.getId());
                logger.info("Tenant creado con ID: {}", tenant.getId());

            } else {
                tenant = tenantService.findById(request.getTenantId());
                logger.info("Usuario uniéndose a tenant existente: {}", tenant.getId());
            }

            // Crear usuario vinculado al tenant
            User user = new User();
            user.setNombre(request.getNombre());
            user.setEmail(request.getEmail());
            user.setPassword(request.getPassword()); // UserService lo encripta
            user.setTenant(tenant);
            user.setRole("ADMIN");
            user = userService.save(user);

            // Generar JWT con tenantId embebido (CRÍTICO para multi-tenant)
            final String token = jwtTokenUtil.generateToken(
                    user.getEmail(),
                    tenant.getId(),
                    user.getRole()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", UserDTO.fromUser(user));
            response.put("tenant", TenantDTO.fromTenant(tenant));
            response.put("message", "Registro exitoso - Sistema multi-tenant configurado");

            logger.info("=== REGISTRO EXITOSO - Tenant: {}, Usuario: {} ===",
                    tenant.getId(), user.getEmail());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error en registro multi-tenant: ", e);
            return ResponseEntity.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Error en registro",
                            "detail", e.getMessage()
                    ));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestHeader("Authorization") String token) {
        String jwtToken = token.substring(7);
        String username = jwtTokenUtil.extractUsername(jwtToken);
        String tenantId = jwtTokenUtil.extractTenantId(jwtToken);

        User user = userService.findByEmail(username);
        String newToken = jwtTokenUtil.generateToken(
                username,
                tenantId,
                user.getRole()
        );

        return ResponseEntity.ok(Map.of("token", newToken));
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.substring(7);
            String username = jwtTokenUtil.extractUsername(jwtToken);

            if (jwtTokenUtil.validateToken(jwtToken, username)) {
                return ResponseEntity.ok(Map.of("valid", true));
            }
        } catch (Exception e) {
            // Token inválido
        }

        return ResponseEntity.ok(Map.of("valid", false));
    }
}