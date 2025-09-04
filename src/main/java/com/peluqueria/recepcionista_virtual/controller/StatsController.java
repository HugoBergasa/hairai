package com.peluqueria.recepcionista_virtual.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.peluqueria.recepcionista_virtual.service.StatsService;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@CrossOrigin
public class StatsController {

    @Autowired
    private StatsService statsService;

    @GetMapping
    public ResponseEntity<?> getStats(@RequestAttribute(required = false) String tenantId) {
        Map<String, Object> stats = statsService.getStatsByTenant(tenantId);
        return ResponseEntity.ok(stats);
    }
}