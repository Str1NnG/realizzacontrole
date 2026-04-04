// Local: src/main/java/com/kodarsoftwares/nivelar/dto/MaquinaDesempenhoDTO.java
package com.kodarsoftwares.nivelar.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor // Importante: cria um construtor com todos os campos
public class MaquinaDesempenhoDTO {
    private String nomeMaquina;
    private Double totalHoras;
}