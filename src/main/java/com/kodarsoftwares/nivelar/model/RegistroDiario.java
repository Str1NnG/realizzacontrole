package com.kodarsoftwares.nivelar.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // IMPORT DA NOSSA BLINDAGEM
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Data
@Entity
public class RegistroDiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate data; // Data do apontamento (informada pelo usuário/app)
    private Double horimetroInicial;
    private Double horimetroFinal;
    private Double abastecimento;

    @Column(length = 1000)
    private String descricao;

    private String caminhoAnexo; // Anexo principal

    // --- Novos Campos ---
    @Column(name = "servico")
    private String servico;

    @Column(name = "cliente")
    private String cliente;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "caminho_anexo_inicio")
    private String caminhoAnexoInicio; // Anexo da foto de início

    @Column(name = "caminho_anexo_final")
    private String caminhoAnexoFinal; // Anexo da foto de final

    // --- Campos Financeiros ---
    @Column(name = "status_pagamento", nullable = false)
    private String statusPagamento = "PENDENTE"; 

    @Column(name = "valor_faturado")
    private Double valorFaturado = 0.0; 

    // --- Campos de Timestamp (Auditoria) ---
    @CreationTimestamp
    @Column(updatable = false) 
    private LocalDateTime dataCriacao; // Data e hora que o registro foi criado no banco

    @UpdateTimestamp
    private LocalDateTime dataAtualizacao; // Data e hora da última modificação


    // =================================================================
    // RELACIONAMENTOS COM A BLINDAGEM JSON (ANTI-CRASH)
    // =================================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operador_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // IGNORA A PREGUIÇA DO BANCO
    private Operador operador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // IGNORA A PREGUIÇA DO BANCO
    private Cliente clienteEntity;

    // Construtores, Getters e Setters são gerados pelo Lombok (@Data)
}