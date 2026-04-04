package com.kodarsoftwares.nivelar.config;

import com.kodarsoftwares.nivelar.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
// Importar AntPathMatcher para verificar rotas
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher; // <-- IMPORTAR
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays; // <-- IMPORTAR para lista de paths

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    // Usado para comparar padrões de URL (como /api/files/**)
    private final AntPathMatcher pathMatcher = new AntPathMatcher(); // <-- NOVO

    // --- NOVO: Lista de paths públicos que o filtro deve IGNORAR ---
    private final String[] PUBLIC_PATHS = {
            "/api/operadores/login",
            "/api/files/profile-pics/**"
            // Adicione outras rotas públicas aqui se necessário (ex: /public/**)
    };
    // -----------------------------------------------------------


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // --- NOVO: Verifica se o path é público ---
        String requestPath = request.getServletPath();
        boolean isPublicPath = Arrays.stream(PUBLIC_PATHS)
                .anyMatch(path -> pathMatcher.match(path, requestPath));

        if (isPublicPath) {
            // Se for público, pula toda a lógica de validação do JWT e continua a cadeia
            filterChain.doFilter(request, response);
            return; // <-- IMPORTANTE: Sai do filtro aqui
        }
        // --- FIM DA VERIFICAÇÃO DE PATH PÚBLICO ---


        // --- Lógica JWT (executa apenas para paths NÃO públicos) ---
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userCpf;

        // Se não for path público E não tiver header, bloqueia (ou continua, dependendo da config)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Para rotas não-públicas, um token é geralmente esperado.
            // Se chegar aqui sem token, a autorização mais abaixo vai barrar (com 401 ou 403).
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        try { // <-- Adiciona try-catch para erros de extração/validação
            userCpf = jwtService.extractUsername(jwt);

            if (userCpf != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userCpf);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // Não passamos credenciais (senha) aqui
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    // Define o usuário como autenticado no contexto de segurança
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
                // Se o token for inválido (jwtService.isTokenValid retorna false),
                // o SecurityContextHolder continua sem autenticação, e as regras
                // de autorização mais abaixo (ex: .authenticated()) irão barrar.
            }
        } catch (Exception e) {
            // Se ocorrer erro ao extrair username ou validar token (ex: expirado, malformado)
            // Limpa o contexto para garantir que não haja autenticação inválida
            SecurityContextHolder.clearContext();
            System.err.println("Erro ao validar token JWT: " + e.getMessage());
            // Poderia retornar 401 aqui, mas deixar passar permite que as regras
            // do SecurityConfig tratem (ex: se alguma rota específica permitisse token inválido)
            // No nosso caso, .authenticated() vai barrar logo em seguida.
        }

        // Continua a cadeia de filtros (para aplicar as regras de autorização do SecurityConfig)
        filterChain.doFilter(request, response);
    }
}