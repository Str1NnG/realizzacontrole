package com.kodarsoftwares.nivelar.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
public class Registro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate data;
    private Double horas;
    private Double latitude;
    private Double longitude;

    @ManyToOne
    private Operador operador;

    @ManyToOne
    private Maquina maquina;
}