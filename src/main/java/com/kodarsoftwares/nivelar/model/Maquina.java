// Local: src/main/java/com/kodarsoftwares/nivelar/model/Maquina.java

package com.kodarsoftwares.nivelar.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // IMPORTANTE ADICIONAR ESTE IMPORT
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // <-- ADICIONE ESTA LINHA
public class Maquina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String modelo;
    private String placaOuSerie;
}