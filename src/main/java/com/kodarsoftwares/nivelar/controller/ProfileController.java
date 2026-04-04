package com.kodarsoftwares.nivelar.controller;

import com.kodarsoftwares.nivelar.model.Operador;
import com.kodarsoftwares.nivelar.repository.OperadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final OperadorRepository operadorRepository;

    // Endpoint para ATUALIZAR o perfil do usuário LOGADO
    @PutMapping
    public ResponseEntity<Operador> updateProfile(@RequestBody Map<String, String> updates, Authentication authentication) {
        // Pega o CPF do usuário que está autenticado (logado)
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userCpf = userDetails.getUsername();

        // Busca o operador no banco de dados
        Operador operador = operadorRepository.findByCpf(userCpf)
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado no banco de dados."));

        // Atualiza os campos se eles foram enviados na requisição
        if (updates.containsKey("nomeEmpresa")) {
            operador.setNomeEmpresa(updates.get("nomeEmpresa"));
        }
        if (updates.containsKey("fotoPerfilUrl")) {
            operador.setFotoPerfilUrl(updates.get("fotoPerfilUrl"));
        }

        // Salva o operador com os dados atualizados
        Operador operadorAtualizado = operadorRepository.save(operador);

        return ResponseEntity.ok(operadorAtualizado);
    }

    @GetMapping
    public ResponseEntity<Operador> getProfile(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userCpf = userDetails.getUsername();

        Operador operador = operadorRepository.findByCpf(userCpf)
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado."));

        return ResponseEntity.ok(operador);
    }
}