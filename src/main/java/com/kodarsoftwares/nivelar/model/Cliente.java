package com.kodarsoftwares.nivelar.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String localServico; // O "Apelido" da localização (ex: Fazenda do Tião)
    private Double valorHora;

    // Campos da Cerca Virtual
    private Double latitude;
    private Double longitude;
    private Integer raioTolerancia; // Em metros
}