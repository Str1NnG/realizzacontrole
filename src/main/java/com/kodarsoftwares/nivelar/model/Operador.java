package com.kodarsoftwares.nivelar.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Data
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Operador implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Column(unique = true, nullable = false)
    private String cpf;

    // AQUI ESTÁ A CORREÇÃO CRÍTICA:
    // 1. nullable = true -> Permite que o objeto seja salvo no banco mesmo que a senha seja nula (vinda do App).
    // 2. Access.WRITE_ONLY -> Permite que o Spring receba a senha no Login, mas nunca a envie de volta.
    @Column(nullable = true)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) 
    private String senha;

    private String cargo; 

    private String nomeEmpresa;
    
    @Column(length = 1000)
    private String fotoPerfilUrl;

    @OneToOne
    @JoinColumn(name = "maquina_id", referencedColumnName = "id")
    private Maquina maquina;

    // --- MÉTODOS UserDetails (Mantidos para o Login funcionar) ---
    @Override
    @JsonIgnore 
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.cargo == null) return Collections.emptyList();
        return Collections.singletonList(new SimpleGrantedAuthority(this.cargo));
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return this.senha; 
    }

    @Override
    @JsonIgnore
    public String getUsername() {
        return this.cpf; 
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}