package com.kodarsoftwares.nivelar.config;

import com.kodarsoftwares.nivelar.model.Operador;
import com.kodarsoftwares.nivelar.repository.OperadorRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(OperadorRepository repository, PasswordEncoder passwordEncoder) {
        return args -> {
            String cpfDono = "70122242408";
            Optional<Operador> donoExistente = repository.findByCpf(cpfDono);

            if (donoExistente.isEmpty()) {
                // SE NÃO EXISTE, CRIA DO ZERO
                Operador admin = new Operador();
                admin.setNome("Paulo");
                admin.setCpf(cpfDono);
                admin.setSenha(passwordEncoder.encode(cpfDono)); 
                admin.setCargo("ROLE_ADMIN");
                admin.setNomeEmpresa("REALIZZA MAIS ENGENHARIA");
                repository.save(admin);
                System.out.println(">>> Administrador principal (Paulo) CRIADO com senha BCrypt.");
            } else {
                // SE JÁ EXISTE, VAMOS FORÇAR A ATUALIZAÇÃO DA SENHA
                Operador paulo = donoExistente.get();
                
                // Verifica se a senha salva NÃO começa com o prefixo do BCrypt ($2a$)
                if (paulo.getSenha() == null || !paulo.getSenha().startsWith("$2a$")) {
                    paulo.setSenha(passwordEncoder.encode(cpfDono));
                    paulo.setCargo("ROLE_ADMIN"); // Garante que a Role esteja certa
                    repository.save(paulo);
                    System.out.println(">>> Senha do Paulo ATUALIZADA para BCrypt com sucesso!");
                }
            }
        };
    }
}