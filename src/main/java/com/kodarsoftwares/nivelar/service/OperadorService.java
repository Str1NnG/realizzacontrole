package com.kodarsoftwares.nivelar.service;

import com.kodarsoftwares.nivelar.model.Maquina; // Importar Maquina
import com.kodarsoftwares.nivelar.model.Operador;
import com.kodarsoftwares.nivelar.repository.MaquinaRepository; // Importar MaquinaRepository
import com.kodarsoftwares.nivelar.repository.OperadorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value; // <-- MUDANÇA 1: Importar o @Value
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils; // Importar StringUtils
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import jakarta.annotation.PostConstruct; // Para criar o diretório na inicialização
import java.net.MalformedURLException; // Necessário para UrlResource (embora não usado diretamente aqui)
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.BadCredentialsException;

@Service
@RequiredArgsConstructor
public class OperadorService {

    private final OperadorRepository operadorRepository;
    private final MaquinaRepository maquinaRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${nivelar.upload-dir.profile-pics}")
    private String uploadDirProfilePics;

    private Path profilePicLocation;

    @PostConstruct
    public void init() {
        try {
            this.profilePicLocation = Paths.get(this.uploadDirProfilePics);
            Files.createDirectories(profilePicLocation);
            System.out.println(">>> Diretório de fotos de perfil criado/verificado em: " + profilePicLocation.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível criar o diretório de upload de fotos de perfil!", e);
        }
    }

    @Transactional
    public Operador createOperador(Operador operadorInput) {
        if (operadorRepository.findByCpf(operadorInput.getCpf()).isPresent()) {
            throw new IllegalArgumentException("CPF já cadastrado.");
        }
        if (operadorInput.getCargo() == null ||
                (!operadorInput.getCargo().equals("ROLE_ADMIN") && !operadorInput.getCargo().equals("ROLE_OPERADOR"))) {
            throw new IllegalArgumentException("Cargo inválido. Use ROLE_ADMIN ou ROLE_OPERADOR.");
        }

        Operador novoOperador = new Operador();
        novoOperador.setNome(operadorInput.getNome());
        novoOperador.setCpf(operadorInput.getCpf());
        novoOperador.setCargo(operadorInput.getCargo());
        novoOperador.setNomeEmpresa(operadorInput.getNomeEmpresa());
        novoOperador.setFotoPerfilUrl(operadorInput.getFotoPerfilUrl());

        // Lógica de criptografia na criação
        if (operadorInput.getSenha() != null && !operadorInput.getSenha().trim().isEmpty()) {
            novoOperador.setSenha(passwordEncoder.encode(operadorInput.getSenha()));
        } else if ("ROLE_ADMIN".equals(operadorInput.getCargo())) {
            throw new IllegalArgumentException("Senha é obrigatória para administradores.");
        } else {
            novoOperador.setSenha(null);
        }

        if (operadorInput.getMaquina() != null && operadorInput.getMaquina().getId() != null) {
            Maquina maquina = maquinaRepository.findById(operadorInput.getMaquina().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Máquina não encontrada com ID: " + operadorInput.getMaquina().getId()));
            novoOperador.setMaquina(maquina);
        } else {
            novoOperador.setMaquina(null);
        }

        return operadorRepository.save(novoOperador);
    }

    @Transactional(readOnly = true)
    public List<Operador> findAllOperadores() {
        return operadorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Operador> findOperadorById(Long id) {
        return operadorRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Operador> findOperadorByCpf(String cpf) {
        return operadorRepository.findByCpf(cpf);
    }

    @Transactional
    public Operador updateOperador(Long id, Operador operadorDetails) {
        Operador operador = operadorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Operador não encontrado com id: " + id));

        operador.setNome(operadorDetails.getNome());
        operador.setNomeEmpresa(operadorDetails.getNomeEmpresa());
        
        // Atualiza o CPF se for enviado
        if (operadorDetails.getCpf() != null && !operadorDetails.getCpf().isEmpty()) {
            operador.setCpf(operadorDetails.getCpf());
        }

        if (operadorDetails.getCargo() != null &&
                (operadorDetails.getCargo().equals("ROLE_ADMIN") || operadorDetails.getCargo().equals("ROLE_OPERADOR"))) {
            operador.setCargo(operadorDetails.getCargo());
        }

        // --- A MÁGICA ACONTECE AQUI ---
        // Se a senha na Payload vier nula ou vazia, o IF pula e MANTÉM a senha atual no banco!
        if (operadorDetails.getSenha() != null && !operadorDetails.getSenha().trim().isEmpty()) {
            operador.setSenha(passwordEncoder.encode(operadorDetails.getSenha()));
        }

        if (operadorDetails.getMaquina() != null && operadorDetails.getMaquina().getId() != null) {
            Maquina maquina = maquinaRepository.findById(operadorDetails.getMaquina().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Máquina não encontrada com ID: " + operadorDetails.getMaquina().getId()));
            operador.setMaquina(maquina);
        } else {
            operador.setMaquina(null);
        }

        return operadorRepository.save(operador);
    }

    @Transactional
    public void deleteOperador(Long id) {
        Operador operador = operadorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Operador não encontrado com id: " + id));
        // TODO: Deletar foto de perfil do disco antes de deletar o operador
        operadorRepository.delete(operador);
    }

    @Transactional
    public void changePassword(Long id, String senhaAtual, String novaSenha) {
        Operador operador = operadorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Operador não encontrado com id: " + id));

        if (!"ROLE_ADMIN".equals(operador.getCargo())) {
            throw new BadCredentialsException("Operadores não podem alterar senha.");
        }

        if (!passwordEncoder.matches(senhaAtual, operador.getPassword())) {
            throw new BadCredentialsException("Senha atual incorreta.");
        }

        if (novaSenha == null || novaSenha.length() < 6) { // Exemplo de validação
            throw new IllegalArgumentException("Nova senha deve ter pelo menos 6 caracteres.");
        }

        operador.setSenha(passwordEncoder.encode(novaSenha));
        operadorRepository.save(operador);
    }

    // Método para o AuthService do Angular usar (exemplo)
    @Transactional
    public Operador updateProfile(Long id, Operador profileData) {
        Operador operador = operadorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Operador não encontrado com id: " + id));

        // Atualiza apenas os campos permitidos do perfil
        if (profileData.getNomeEmpresa() != null) {
            operador.setNomeEmpresa(profileData.getNomeEmpresa());
        }
        // Foto é atualizada pelo método updateFotoPerfil

        return operadorRepository.save(operador);
    }

    // --- NOVO MÉTODO: updateFotoPerfil ---
    @Transactional
    public Operador updateFotoPerfil(Long id, MultipartFile file) throws IOException {
        Operador operador = operadorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Operador não encontrado com id: " + id));

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo de imagem não pode ser vazio.");
        }
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

        if (originalFilename == null || originalFilename.contains("..")) {
            throw new IllegalArgumentException("Nome de arquivo inválido: " + originalFilename);
        }

        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (!"jpg".equalsIgnoreCase(extension) && !"jpeg".equalsIgnoreCase(extension) && !"png".equalsIgnoreCase(extension)) {
            throw new IllegalArgumentException("Formato de arquivo inválido. Use JPG ou PNG.");
        }

        String filename = id + "." + extension;
        Path destinationFile = this.profilePicLocation.resolve(Paths.get(filename))
                .normalize().toAbsolutePath();

        // Garante que o diretório de destino seja o esperado (segurança extra)
        if (!destinationFile.getParent().equals(this.profilePicLocation.toAbsolutePath())) {
            throw new IOException("Não é possível salvar arquivo fora do diretório de fotos de perfil.");
        }


        // Exclui a foto antiga se existir
        if (operador.getFotoPerfilUrl() != null && !operador.getFotoPerfilUrl().isEmpty()) {
            try {
                Path oldFilePath = this.profilePicLocation.resolve(operador.getFotoPerfilUrl()).normalize();
                if (Files.exists(oldFilePath) && !Files.isDirectory(oldFilePath)) {
                    Files.deleteIfExists(oldFilePath);
                }
            } catch (IOException e) {
                System.err.println("Aviso: Falha ao deletar foto de perfil antiga: " + operador.getFotoPerfilUrl() + " - " + e.getMessage());
            }
        }

        // Copia o novo arquivo
        Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

        // Atualiza a entidade Operador com o NOME do arquivo
        operador.setFotoPerfilUrl(filename);

        return operadorRepository.save(operador);
    }
}