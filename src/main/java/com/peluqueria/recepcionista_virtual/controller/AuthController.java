package com.peluqueria.recepcionista_virtual.controller;

import com.peluqueria.dto.*;
import com.peluqueria.recepcionista_virtual.model.*;
import com.peluqueria.recepcionista_virtual.security.JwtTokenUtil;
import com.peluqueria.recepcionista_virtual.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

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
        // Verificar si el email ya existe
        if (userService.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El email ya está registrado"));
        }

        // Crear nuevo tenant si es necesario
        Tenant tenant;
        if (request.isNewTenant()) {
            tenant = new Tenant();
            tenant.setNombrePeluqueria(request.getNombrePeluqueria());
            tenant.setTelefono(request.getTelefono());
            tenant.setDireccion(request.getDireccion());
            tenant = tenantService.save(tenant);

            // Crear servicios predeterminados
            tenantService.createDefaultServices(tenant.getId());
        } else {
            tenant = tenantService.findById(request.getTenantId());
        }

        // Crear usuario
        User user = new User();
        user.setNombre(request.getNombre());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setTenant(tenant);
        user.setRole("ADMIN");
        user = userService.save(user);

        // Generar token
        final String token = jwtTokenUtil.generateToken(
                user.getEmail(),
                tenant.getId(),
                user.getRole()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", UserDTO.fromUser(user));
        response.put("tenant", TenantDTO.fromTenant(tenant));

        return ResponseEntity.ok(response);
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