// Local: src/main/java/com/kodarsoftwares/nivelar/dto/FuncionarioDesempenhoDTO.java
package com.kodarsoftwares.nivelar.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor // Importante: cria um construtor com todos os campos
public class FuncionarioDesempenhoDTO {
    private String nomeFuncionario;
    private Double totalHoras;
}