package com.peluqueria.recepcionista_virtual.service;

import com.peluqueria.recepcionista_virtual.model.*;
import com.peluqueria.recepcionista_virtual.repository.*;
import com.peluqueria.recepcionista_virtual.dto.ClienteDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TenantRepository tenantRepository;

    /**
     * 🔒 MÉTODO SEGURO - Solo clientes del tenant
     */
    public List<ClienteDTO> getClientesByTenantId(String tenantId) {
        List<Cliente> clientes = clienteRepository.findByTenantId(tenantId);
        return clientes.stream()
                .map(ClienteDTO::fromCliente)
                .collect(Collectors.toList());
    }

    /**
     * 🔍 BÚSQUEDA DE CLIENTES POR TENANT - NUEVO MÉTODO
     */
    public List<ClienteDTO> searchClientesByTenantId(String tenantId, String search) {
        if (search == null || search.trim().isEmpty()) {
            return getClientesByTenantId(tenantId);
        }

        List<Cliente> clientes = clienteRepository.findByTenantId(tenantId);

        // Filtrar por nombre, teléfono o email que contenga el término de búsqueda
        String searchLower = search.toLowerCase().trim();

        return clientes.stream()
                .filter(cliente ->
                        (cliente.getNombre() != null && cliente.getNombre().toLowerCase().contains(searchLower)) ||
                                (cliente.getTelefono() != null && cliente.getTelefono().contains(searchLower)) ||
                                (cliente.getEmail() != null && cliente.getEmail().toLowerCase().contains(searchLower))
                )
                .map(ClienteDTO::fromCliente)
                .collect(Collectors.toList());
    }

    /**
     * 📱 CREAR CLIENTE - NUEVO MÉTODO
     */
    public ClienteDTO createCliente(String tenantId, ClienteDTO clienteDTO) {
        // Validar que el tenant existe
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

        // Verificar si ya existe un cliente con ese teléfono en el tenant
        if (clienteDTO.getTelefono() != null) {
            clienteRepository.findByTelefonoAndTenantId(clienteDTO.getTelefono(), tenantId)
                    .ifPresent(existingCliente -> {
                        throw new RuntimeException("Ya existe un cliente con ese teléfono");
                    });
        }

        // Crear nuevo cliente
        Cliente cliente = new Cliente();
        cliente.setTenant(tenant);
        cliente.setNombre(clienteDTO.getNombre());
        cliente.setTelefono(clienteDTO.getTelefono());
        cliente.setEmail(clienteDTO.getEmail());
        cliente.setNotas(clienteDTO.getNotas());
        cliente.setFechaRegistro(LocalDateTime.now());

        // Guardar cliente
        Cliente clienteGuardado = clienteRepository.save(cliente);
        return ClienteDTO.fromCliente(clienteGuardado);
    }

    /**
     * ✏️ ACTUALIZAR CLIENTE - NUEVO MÉTODO
     */
    public ClienteDTO updateCliente(String clienteId, ClienteDTO clienteDTO) {
        // Buscar cliente existente
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        // Validar cambio de teléfono (no debe duplicarse en el mismo tenant)
        if (clienteDTO.getTelefono() != null &&
                !clienteDTO.getTelefono().equals(cliente.getTelefono())) {

            clienteRepository.findByTelefonoAndTenantId(clienteDTO.getTelefono(), cliente.getTenant().getId())
                    .ifPresent(existingCliente -> {
                        if (!existingCliente.getId().equals(clienteId)) {
                            throw new RuntimeException("Ya existe otro cliente con ese teléfono");
                        }
                    });
        }

        // Actualizar campos si están presentes
        if (clienteDTO.getNombre() != null) {
            cliente.setNombre(clienteDTO.getNombre());
        }
        if (clienteDTO.getTelefono() != null) {
            cliente.setTelefono(clienteDTO.getTelefono());
        }
        if (clienteDTO.getEmail() != null) {
            cliente.setEmail(clienteDTO.getEmail());
        }
        if (clienteDTO.getNotas() != null) {
            cliente.setNotas(clienteDTO.getNotas());
        }

        // Guardar cambios
        Cliente clienteActualizado = clienteRepository.save(cliente);
        return ClienteDTO.fromCliente(clienteActualizado);
    }

    /**
     * 📋 OBTENER CLIENTE POR ID - MÉTODO EXISTENTE
     */
    public ClienteDTO getClienteById(String clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        return ClienteDTO.fromCliente(cliente);
    }

    /**
     * 📊 MÉTODO ADICIONAL - Estadísticas de cliente
     */
    public ClienteDTO getClienteConEstadisticas(String clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        ClienteDTO clienteDTO = ClienteDTO.fromCliente(cliente);

        // Aquí podrías añadir estadísticas adicionales como:
        // - Número de citas históricas
        // - Gasto total
        // - Servicios más utilizados
        // Esto requerirá queries adicionales al CitaRepository

        return clienteDTO;
    }

    /**
     * 🔍 BUSCAR CLIENTE POR TELÉFONO EN TENANT ESPECÍFICO
     */
    public ClienteDTO findByTelefonoAndTenant(String telefono, String tenantId) {
        return clienteRepository.findByTelefonoAndTenantId(telefono, tenantId)
                .map(ClienteDTO::fromCliente)
                .orElse(null);
    }

    /**
     * 📈 OBTENER CLIENTES NUEVOS DEL MES
     */
    public List<ClienteDTO> getClientesNuevosDelMes(String tenantId) {
        LocalDateTime inicioMes = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime finMes = inicioMes.plusMonths(1).minusSeconds(1);

        List<Cliente> clientes = clienteRepository.findByTenantId(tenantId);

        return clientes.stream()
                .filter(cliente -> cliente.getFechaRegistro() != null &&
                        cliente.getFechaRegistro().isAfter(inicioMes) &&
                        cliente.getFechaRegistro().isBefore(finMes))
                .map(ClienteDTO::fromCliente)
                .collect(Collectors.toList());
    }

    /**
     * 📱 VALIDAR TELÉFONO ÚNICO EN TENANT
     */
    public boolean isTelefonoDisponible(String telefono, String tenantId, String excludeClienteId) {
        return clienteRepository.findByTelefonoAndTenantId(telefono, tenantId)
                .map(cliente -> cliente.getId().equals(excludeClienteId))
                .orElse(true);
    }
}