package com.kodarsoftwares.nivelar.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configure(http)) 
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                .requestMatchers("/api/operadores/login").permitAll()
                                
                                // ---> A MÁGICA DE LIBERAÇÃO DAS FOTOS AQUI <---
                                .requestMatchers(HttpMethod.GET, 
                                    "/api/files/profile-pics/**", 
                                    "/api/files/anexos/**",
                                    "/files/profile-pics/**", 
                                    "/files/anexos/**"
                                ).permitAll()
                                
                                .requestMatchers(HttpMethod.POST, "/api/operadores").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/operadores", "/api/operadores/{id}").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.PUT, "/api/operadores/{id}").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/api/operadores/{id}").hasRole("ADMIN")
                                .requestMatchers("/api/operadores/profile-picture", "/api/operadores/change-password", "/api/operadores/profile").authenticated()
                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                .requestMatchers("/api/reports/**").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/registros", "/api/registros/{id}").authenticated()
                                .requestMatchers(HttpMethod.PUT, "/api/registros/{id}").authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/api/registros/{id}").authenticated()
                                .requestMatchers(HttpMethod.POST, "/api/registros").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/registros/operador/{operadorId}").authenticated()
                                .requestMatchers("/api/registros/{id}/anexo").authenticated()
                                .requestMatchers("/api/maquinas/**").hasRole("ADMIN")
                                .anyRequest().permitAll()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Access-Control-Allow-Origin"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}