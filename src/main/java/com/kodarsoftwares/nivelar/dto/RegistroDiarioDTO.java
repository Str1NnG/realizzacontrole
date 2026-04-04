package com.kodarsoftwares.nivelar.dto;

import com.fasterxml.jackson.annotation.JsonFormat; 
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime; 

@Data
public class RegistroDiarioDTO {
    private Long id;
    private LocalDate data;
    private Double horimetroInicial;
    private Double horimetroFinal;
    private Double abastecimento;
    private String descricao;
    private String caminhoAnexo;

    private String servico;
    private String cliente;
    private Double latitude;
    private Double longitude;
    private String caminhoAnexoInicio;
    private String caminhoAnexoFinal;

    private String statusPagamento;
    private Double valorFaturado;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dataCriacao;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dataAtualizacao;

    private OperadorInfo operador;
    
    // --- NOVO RETORNO DO CLIENTE ---
    private ClienteInfo clienteDetalhe;

    @Data
    public static class OperadorInfo {
        private Long id;
        private String nome;
        private MaquinaInfo maquina;
    }

    @Data
    public static class MaquinaInfo {
        private String nome;
    }

    @Data
    public static class ClienteInfo {
        private Long id;
        private String nome;
        private String localServico;
        private Double valorHora;
    }
}