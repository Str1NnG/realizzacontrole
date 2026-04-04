package com.kodarsoftwares.nivelar.controller;

import com.kodarsoftwares.nivelar.model.Maquina;
import com.kodarsoftwares.nivelar.model.Operador;
import com.kodarsoftwares.nivelar.model.RegistroDiario;
import com.kodarsoftwares.nivelar.repository.MaquinaRepository;
import com.kodarsoftwares.nivelar.repository.OperadorRepository;
import com.kodarsoftwares.nivelar.repository.RegistroDiarioRepository;
import com.kodarsoftwares.nivelar.service.OperadorService; // <-- IMPORTANTE: O Service injetado aqui
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final RegistroDiarioRepository registroDiarioRepository;
    private final OperadorRepository operadorRepository;
    private final MaquinaRepository maquinaRepository;
    private final OperadorService operadorService; // <-- IMPORTANTE: Variável do Service

    // --- ENDPOINTS DE RELATÓRIOS (JÁ EXISTENTES) ---
    @GetMapping("/registros/all")
    public ResponseEntity<List<RegistroDiario>> getAllRegistros() {
        return ResponseEntity.ok(registroDiarioRepository.findAll());
    }

    // --- GERENCIAMENTO DE OPERADORES ---

    @GetMapping("/operadores/all")
    public ResponseEntity<List<Operador>> getAllOperadores() {
        return ResponseEntity.ok(operadorRepository.findAll());
    }

    @PostMapping("/operadores")
    public ResponseEntity<Operador> createOperador(@RequestBody Operador operador) {
        // CORREÇÃO: Usa o Service para garantir que a senha seja criptografada ao criar!
        Operador novoOperador = operadorService.createOperador(operador);
        return new ResponseEntity<>(novoOperador, HttpStatus.CREATED);
    }

    @PutMapping("/operadores/{id}")
    public ResponseEntity<Operador> updateOperador(@PathVariable Long id, @RequestBody Operador operadorAtualizado) {
        try {
            // CORREÇÃO: Usa o Service para a senha não ser apagada se o JSON vier null!
            Operador atualizado = operadorService.updateOperador(id, operadorAtualizado);
            return ResponseEntity.ok(atualizado);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Operador não encontrado ou erro ao atualizar.");
        }
    }

    @DeleteMapping("/operadores/{id}")
    public ResponseEntity<Void> deleteOperador(@PathVariable Long id) {
        if (!registroDiarioRepository.findByOperadorIdOrderByDataDesc(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível deletar um operador que possui relatórios associados.");
        }
        // Usa o service para deletar corretamente
        operadorService.deleteOperador(id);
        return ResponseEntity.noContent().build();
    }

    // --- GERENCIAMENTO DE MÁQUINAS ---

    @GetMapping("/maquinas/all")
    public ResponseEntity<List<Maquina>> getAllMaquinas() {
        return ResponseEntity.ok(maquinaRepository.findAll());
    }

    @PostMapping("/maquinas")
    public ResponseEntity<Maquina> createMaquina(@RequestBody Maquina maquina) {
        Maquina novaMaquina = maquinaRepository.save(maquina);
        return new ResponseEntity<>(novaMaquina, HttpStatus.CREATED);
    }

    @PutMapping("/maquinas/{id}")
    public ResponseEntity<Maquina> updateMaquina(@PathVariable Long id, @RequestBody Maquina maquinaAtualizada) {
        return maquinaRepository.findById(id)
                .map(maquina -> {
                    maquina.setNome(maquinaAtualizada.getNome());
                    maquina.setModelo(maquinaAtualizada.getModelo());
                    maquina.setPlacaOuSerie(maquinaAtualizada.getPlacaOuSerie());
                    return ResponseEntity.ok(maquinaRepository.save(maquina));
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Máquina não encontrada"));
    }

    @DeleteMapping("/maquinas/{id}")
    public ResponseEntity<Void> deleteMaquina(@PathVariable Long id) {
        maquinaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}