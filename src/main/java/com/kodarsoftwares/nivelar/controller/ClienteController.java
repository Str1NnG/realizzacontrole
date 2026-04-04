package com.kodarsoftwares.nivelar.controller;

import com.kodarsoftwares.nivelar.model.Cliente;
import com.kodarsoftwares.nivelar.repository.ClienteRepository;
import com.kodarsoftwares.nivelar.service.RegistroDiarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private RegistroDiarioService registroDiarioService; // INJETAMOS O SERVIÇO AQUI

    @GetMapping
    public List<Cliente> getAll() {
        return clienteRepository.findAll();
    }

    @PostMapping
    public Cliente create(@RequestBody Cliente cliente) {
        Cliente clienteSalvo = clienteRepository.save(cliente);
        
        // GATILHO: Dispara a busca retroativa ao criar um cliente novo
        registroDiarioService.vincularRegistrosAntigosAoCliente(clienteSalvo);
        
        return clienteSalvo;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cliente> update(@PathVariable Long id, @RequestBody Cliente detalhes) {
        return clienteRepository.findById(id).map(cliente -> {
            cliente.setNome(detalhes.getNome());
            cliente.setLocalServico(detalhes.getLocalServico());
            cliente.setValorHora(detalhes.getValorHora());
            
            // CORREÇÃO: O seu sistema não estava salvando as mudanças de GPS!
            cliente.setLatitude(detalhes.getLatitude());
            cliente.setLongitude(detalhes.getLongitude());
            cliente.setRaioTolerancia(detalhes.getRaioTolerancia());
            
            Cliente clienteAtualizado = clienteRepository.save(cliente);
            
            // GATILHO: Dispara a busca retroativa se você editar as coordenadas
            registroDiarioService.vincularRegistrosAntigosAoCliente(clienteAtualizado);
            
            return ResponseEntity.ok(clienteAtualizado);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clienteRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}