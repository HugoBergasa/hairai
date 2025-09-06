package com.peluqueria.recepcionista_virtual.service;

import com.peluqueria.recepcionista_virtual.model.*;
import com.peluqueria.recepcionista_virtual.repository.*;
import com.peluqueria.recepcionista_virtual.dto.ClienteDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    /**
     * 🔒 MÉTODO SEGURO - Solo clientes del tenant
     */
    public List<ClienteDTO> getClientesByTenantId(String tenantId) {
        List<Cliente> clientes = clienteRepository.findByTenantId(tenantId);
        return clientes.stream()
                .map(ClienteDTO::fromCliente)
                .collect(Collectors.toList());
    }

    public ClienteDTO getClienteById(String clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        return ClienteDTO.fromCliente(cliente);
    }
}