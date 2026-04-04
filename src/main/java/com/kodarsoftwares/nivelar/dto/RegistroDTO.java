package com.kodarsoftwares.nivelar.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class RegistroDTO {
    private Long id;
    private LocalDate data;
    private String operadorNome;
    private String maquinaNome;
    private Double horas;
    private Double valorTotal;
    
    // CAMPOS RENOMEADOS
    private String clienteNome;      // Ex: "Tião Costa"
    private String localIdentificado; // Ex: "Serviço de Terraplanagem - Lote A"
    private boolean identificadoPeloGps;
}