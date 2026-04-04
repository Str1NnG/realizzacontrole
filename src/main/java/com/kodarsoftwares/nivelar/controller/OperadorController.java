package com.kodarsoftwares.nivelar.controller;

import com.kodarsoftwares.nivelar.model.Maquina; // Import Maquina
import com.kodarsoftwares.nivelar.model.Operador;
import com.kodarsoftwares.nivelar.service.JwtService;
import com.kodarsoftwares.nivelar.service.OperadorService;
import jakarta.persistence.EntityNotFoundException; // Import EntityNotFoundException
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException; // Import BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // Import MultipartFile
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException; // Import IOException
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/operadores")
@RequiredArgsConstructor
public class OperadorController {

    private final OperadorService operadorService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // --- DTOs Internos ---
    @Data
    static class CreateOperadorRequest {
        private String nome;
        private String cpf;
        private String cargo;
        private String senha;
        private String nomeEmpresa;
        private Long maquinaId;
    }

    @Data
    static class UpdateOperadorRequest {
        private String nome;
        private String cargo;
        private String nomeEmpresa;
        private Long maquinaId;
    }

    @Data
    static class ChangePasswordRequest {
        private String senhaAtual;
        private String novaSenha;
    }
    // ---------------------


    @PostMapping("/login")
        public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
            String cpf = loginRequest.get("cpf");
            String senha = loginRequest.get("senha");

            if (cpf == null || cpf.isEmpty()) {
                return ResponseEntity.badRequest().body("CPF é obrigatório.");
            }

            Operador operadorAutenticado;

            // SE A SENHA NÃO VIER (App em produção), BUSCA DIRETO PELO CPF
            if (senha == null || senha.trim().isEmpty()) {
                operadorAutenticado = operadorService.findOperadorByCpf(cpf)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Operador não encontrado."));
            } 
            // SE A SENHA VIER (Painel Web), VALIDA NO AUTHENTICATION MANAGER
            else {
                try {
                    Authentication authentication = authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(cpf, senha)
                    );
                    operadorAutenticado = (Operador) authentication.getPrincipal();
                } catch (AuthenticationException e) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "CPF ou Senha inválidos.");
                }
            }

            var token = jwtService.generateToken(operadorAutenticado);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("token", token);
            responseBody.put("operador", operadorAutenticado);

            return ResponseEntity.ok(responseBody);
        }

    @PostMapping
    public ResponseEntity<?> createOperador(@RequestBody CreateOperadorRequest request) {
        try {
            Operador operadorInput = new Operador();
            operadorInput.setNome(request.getNome());
            operadorInput.setCpf(request.getCpf());
            operadorInput.setCargo(request.getCargo());
            operadorInput.setSenha(request.getSenha());
            operadorInput.setNomeEmpresa(request.getNomeEmpresa());
            if(request.getMaquinaId() != null) {
                Maquina m = new Maquina(); m.setId(request.getMaquinaId());
                operadorInput.setMaquina(m);
            }

            Operador novoOperador = operadorService.createOperador(operadorInput);
            return ResponseEntity.status(HttpStatus.CREATED).body(novoOperador);
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace(); // Logar o erro completo no backend
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Erro interno ao criar operador."));
        }
    }

    @GetMapping
    public ResponseEntity<List<Operador>> getAllOperadores() {
        return ResponseEntity.ok(operadorService.findAllOperadores());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Operador> getOperadorById(@PathVariable Long id) {
        return operadorService.findOperadorById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateOperador(@PathVariable Long id, @RequestBody UpdateOperadorRequest request) {
        try {
            Operador operadorDetails = new Operador();
            operadorDetails.setNome(request.getNome());
            operadorDetails.setCargo(request.getCargo());
            operadorDetails.setNomeEmpresa(request.getNomeEmpresa());
            if(request.getMaquinaId() != null) {
                Maquina m = new Maquina(); m.setId(request.getMaquinaId());
                operadorDetails.setMaquina(m);
            } else {
                operadorDetails.setMaquina(null);
            }

            Operador operadorAtualizado = operadorService.updateOperador(id, operadorDetails);
            return ResponseEntity.ok(operadorAtualizado);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace(); // Logar
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Erro interno ao atualizar operador."));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOperador(@PathVariable Long id) {
        try {
            operadorService.deleteOperador(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace(); // Logar
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // Ou INTERNAL_SERVER_ERROR
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changeCurrentUserPassword(@RequestBody ChangePasswordRequest request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userCpf = principal.getName();

        try {
            Operador operador = operadorService.findOperadorByCpf(userCpf)
                    .orElseThrow(() -> new EntityNotFoundException("Usuário autenticado não encontrado."));

            operadorService.changePassword(operador.getId(), request.getSenhaAtual(), request.getNovaSenha());
            return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso."));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (BadCredentialsException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace(); // Logar
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Erro interno ao alterar senha."));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateCurrentUserProfile(@RequestBody Map<String, String> profileData, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userCpf = principal.getName();

        try {
            Operador operador = operadorService.findOperadorByCpf(userCpf)
                    .orElseThrow(() -> new EntityNotFoundException("Usuário autenticado não encontrado."));

            Operador dadosAtualizacao = new Operador();
            // Permite atualizar apenas o nome da empresa por este endpoint
            dadosAtualizacao.setNomeEmpresa(profileData.get("nomeEmpresa"));

            Operador operadorAtualizado = operadorService.updateProfile(operador.getId(), dadosAtualizacao);
            return ResponseEntity.ok(operadorAtualizado);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace(); // Logar
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Erro interno ao atualizar perfil."));
        }
    }

    // --- NOVO ENDPOINT: Upload Foto Perfil ---
    @PostMapping("/profile-picture")
    public ResponseEntity<?> uploadProfilePicture(@RequestPart("file") MultipartFile file, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Usuário não autenticado."));
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Arquivo não enviado ou vazio."));
        }

        String userCpf = principal.getName();

        try {
            Operador operador = operadorService.findOperadorByCpf(userCpf)
                    .orElseThrow(() -> new EntityNotFoundException("Usuário autenticado não encontrado."));

            Operador operadorAtualizado = operadorService.updateFotoPerfil(operador.getId(), file);

            // Retorna apenas a URL da foto (nome do arquivo)
            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("fotoPerfilUrl", operadorAtualizado.getFotoPerfilUrl());

            return ResponseEntity.ok(responseBody);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IOException e) {
            System.err.println("Erro de IO ao salvar foto de perfil: " + e.getMessage());
            e.printStackTrace(); // Logar
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Erro ao salvar a imagem no servidor."));
        } catch (Exception e) {
            System.err.println("Erro inesperado ao salvar foto de perfil: " + e.getMessage());
            e.printStackTrace(); // Logar
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Erro inesperado ao processar a imagem."));
        }
    }
}