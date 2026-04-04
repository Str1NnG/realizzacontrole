package com.kodarsoftwares.nivelar.config;

import com.kodarsoftwares.nivelar.repository.OperadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
// Removido AuthenticationProvider e DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final OperadorRepository operadorRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        // Continua igual: busca o Operador (que é UserDetails) pelo CPF
        return username -> operadorRepository.findByCpf(username)
                .orElseThrow(() -> new UsernameNotFoundException("Operador não encontrado com o CPF: " + username));
    }

    // --- REMOVIDO o @Bean AuthenticationProvider antigo ---
    // @Bean
    // public AuthenticationProvider authenticationProvider() {
    //    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    //    authProvider.setUserDetailsService(userDetailsService());
    //    authProvider.setPasswordEncoder(passwordEncoder());
    //    return authProvider;
    // }
    // O Spring agora vai usar nosso RoleBasedAuthenticationProvider por causa do @Component

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Mantém o BCryptPasswordEncoder
        return new BCryptPasswordEncoder();
    }
}