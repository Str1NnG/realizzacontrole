package com.kodarsoftwares.nivelar.config; // Ou seu pacote de config

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component; // <-- Adicionar @Component

@Component // <-- Torna esta classe um Bean gerenciado pelo Spring
@RequiredArgsConstructor
public class RoleBasedAuthenticationProvider implements AuthenticationProvider {

    private final UserDetailsService userDetailsService; // Carrega o Operador
    private final PasswordEncoder passwordEncoder;     // BCrypt para comparar senhas de Admin

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        // 1. Extrai CPF e Senha da tentativa de login
        String cpf = authentication.getName();
        String senhaFornecida = authentication.getCredentials().toString();

        // 2. Carrega o UserDetails (nosso Operador) pelo CPF
        // Se não encontrar, o userDetailsService já lança UsernameNotFoundException
        UserDetails userDetails = userDetailsService.loadUserByUsername(cpf);

        // 3. Verifica a ROLE (cargo) do usuário
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        // 4. Lógica de Autenticação Baseada na Role
        if (isAdmin) {
            // ADMIN: Precisa verificar a senha com BCrypt
            if (passwordEncoder.matches(senhaFornecida, userDetails.getPassword())) {
                // Senha correta: retorna o token de autenticação
                return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            } else {
                // Senha incorreta
                throw new BadCredentialsException("CPF ou Senha inválidos.");
            }
        } else {
            // OPERADOR: Não verifica a senha, autentica apenas pelo CPF existir
            // (Assumindo que qualquer operador encontrado é válido)
            return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        // Diz ao Spring que este provider suporta o tipo de token que usamos no login
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}