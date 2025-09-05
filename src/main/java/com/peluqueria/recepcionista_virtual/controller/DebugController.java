package com.peluqueria.recepcionista_virtual.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@CrossOrigin
public class DebugController {

    @GetMapping("/tenant")
    public ResponseEntity<?> checkTenantId(HttpServletRequest request) {
        Map<String, Object> debug = new HashMap<>();

        String tenantId = (String) request.getAttribute("tenantId");

        debug.put("tenantId", tenantId);
        debug.put("tenantIdExists", tenantId != null);
        debug.put("authHeader", request.getHeader("Authorization") != null);
        debug.put("path", request.getServletPath());
        debug.put("method", request.getMethod());
        debug.put("timestamp", System.currentTimeMillis());

        System.out.println("=== DEBUG CONTROLLER ===");
        System.out.println("TenantId recibido: " + tenantId);
        System.out.println("Request path: " + request.getServletPath());
        System.out.println("========================");

        return ResponseEntity.ok(debug);
    }
}