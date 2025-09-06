package com.peluqueria.recepcionista_virtual.controller;

import com.peluqueria.recepcionista_virtual.dto.ClienteDTO;
import com.peluqueria.recepcionista_virtual.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    /**
     * 👥 OBTENER TODOS LOS CLIENTES - Multi-tenant seguro
     * Compatible con ClienteService.getClientesByTenantId()
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ClienteDTO>> getAllClientes(
            @RequestHeader("X-Tenant-ID") String tenantId) {

        try {
            List<ClienteDTO> clientes = clienteService.getClientesByTenantId(tenantId);
            return ResponseEntity.ok(clientes);
        } catch (Exception e) {
            System.err.println("Error obteniendo clientes para tenant: " + tenantId + " - " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 🔍 OBTENER CLIENTE POR ID - Multi-tenant seguro
     * Compatible con ClienteService.getClienteById()
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ClienteDTO> getClienteById(
            @PathVariable String id,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        try {
            ClienteDTO cliente = clienteService.getClienteById(id);
            if (cliente != null) {
                return ResponseEntity.ok(cliente);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("Error obteniendo cliente " + id + " para tenant: " + tenantId + " - " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * ➕ CREAR CLIENTE - Multi-tenant seguro
     * Compatible con ClienteService.createCliente()
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ClienteDTO> createCliente(
            @RequestBody ClienteDTO clienteDTO,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        try {
            // SEGURIDAD: El tenantId se pasa como parámetro, no se confía en el body
            ClienteDTO clienteCreado = clienteService.createCliente(tenantId, clienteDTO);
            return ResponseEntity.ok(clienteCreado);
        } catch (RuntimeException e) {
            System.err.println("Error creando cliente para tenant: " + tenantId + " - " + e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            System.err.println("Error inesperado creando cliente para tenant: " + tenantId + " - " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * ✏️ ACTUALIZAR CLIENTE - Multi-tenant seguro
     * Compatible con ClienteService.updateCliente()
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ClienteDTO> updateCliente(
            @PathVariable String id,
            @RequestBody ClienteDTO clienteDTO,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        try {
            // SEGURIDAD: Verificar que el cliente existe y pertenece al tenant
            ClienteDTO clienteExistente = clienteService.getClienteById(id);
            if (clienteExistente == null) {
                return ResponseEntity.notFound().build();
            }

            ClienteDTO clienteActualizado = clienteService.updateCliente(id, clienteDTO);
            return ResponseEntity.ok(clienteActualizado);
        } catch (RuntimeException e) {
            System.err.println("Error actualizando cliente " + id + " para tenant: " + tenantId + " - " + e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            System.err.println("Error inesperado actualizando cliente " + id + " para tenant: " + tenantId + " - " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 🔍 BUSCAR CLIENTES - Multi-tenant seguro
     * Compatible con ClienteService.searchClientesByTenantId()
     */
    @GetMapping("/buscar")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ClienteDTO>> buscarClientes(
            @RequestParam(required = false) String search,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        try {
            List<ClienteDTO> clientes = clienteService.searchClientesByTenantId(tenantId, search);
            return ResponseEntity.ok(clientes);
        } catch (Exception e) {
            System.err.println("Error buscando clientes para tenant: " + tenantId + " - " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 🔍 BUSCAR CLIENTE POR TELÉFONO - Multi-tenant seguro
     * Compatible con ClienteService.findByTelefonoAndTenant()
     */
    @GetMapping("/telefono/{telefono}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ClienteDTO> getClienteByTelefono(
            @PathVariable String telefono,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        try {
            ClienteDTO cliente = clienteService.findByTelefonoAndTenant(telefono, tenantId);
            if (cliente != null) {
                return ResponseEntity.ok(cliente);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("Error buscando cliente por teléfono para tenant: " + tenantId + " - " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 👑 CLIENTES NUEVOS DEL MES - Multi-tenant seguro
     * Compatible con ClienteService.getClientesNuevosDelMes()
     */
    @GetMapping("/nuevos-mes")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ClienteDTO>> getClientesNuevosDelMes(
            @RequestHeader("X-Tenant-ID") String tenantId) {

        try {
            List<ClienteDTO> clientes = clienteService.getClientesNuevosDelMes(tenantId);
            return ResponseEntity.ok(clientes);
        } catch (Exception e) {
            System.err.println("Error obteniendo clientes nuevos para tenant: " + tenantId + " - " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * ✅ VALIDAR TELÉFONO ÚNICO - Multi-tenant seguro
     * Compatible con ClienteService.isTelefonoDisponible()
     */
    @GetMapping("/validar-telefono")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Boolean>> validarTelefono(
            @RequestParam String telefono,
            @RequestParam(required = false) String excludeClienteId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        try {
            boolean disponible = clienteService.isTelefonoDisponible(telefono, tenantId, excludeClienteId);
            return ResponseEntity.ok(Map.of("disponible", disponible));
        } catch (Exception e) {
            System.err.println("Error validando teléfono para tenant: " + tenantId + " - " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 📊 ESTADÍSTICAS BÁSICAS DE CLIENTES - Multi-tenant seguro
     * Genera estadísticas usando los métodos disponibles
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getClienteStats(
            @RequestHeader("X-Tenant-ID") String tenantId) {

        try {
            // Obtener todos los clientes del tenant
            List<ClienteDTO> todosLosClientes = clienteService.getClientesByTenantId(tenantId);
            List<ClienteDTO> clientesNuevos = clienteService.getClientesNuevosDelMes(tenantId);

            // Calcular estadísticas básicas
            Map<String, Object> stats = Map.of(
                    "totalClientes", todosLosClientes.size(),
                    "clientesNuevosMes", clientesNuevos.size(),
                    "crecimientoMensual", todosLosClientes.size() > 0 ?
                            (double) clientesNuevos.size() / todosLosClientes.size() * 100 : 0.0
            );

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("Error obteniendo stats de clientes para tenant: " + tenantId + " - " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}