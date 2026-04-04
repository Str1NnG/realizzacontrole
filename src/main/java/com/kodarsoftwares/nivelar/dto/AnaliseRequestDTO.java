// Local: src/main/java/com/kodarsoftwares/nivelar/dto/AnaliseRequestDTO.java
package com.kodarsoftwares.nivelar.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AnaliseRequestDTO {
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String descricao; // Para filtrar por "serviço Tião Gomes", etc.
}