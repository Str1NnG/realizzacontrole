package com.kodarsoftwares.nivelar.service;

import com.kodarsoftwares.nivelar.dto.RegistroDTO;
import com.kodarsoftwares.nivelar.model.Cliente;
import com.kodarsoftwares.nivelar.model.Registro;
import com.kodarsoftwares.nivelar.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegistroService {

    @Autowired
    private ClienteRepository clienteRepository;

    public RegistroDTO converterParaDTO(Registro reg) {
        RegistroDTO dto = new RegistroDTO();
        dto.setId(reg.getId());
        dto.setData(reg.getData());
        dto.setHoras(reg.getHoras());
        
        // Proteção contra NullPointerException se não houver operador ou máquina
        dto.setOperadorNome(reg.getOperador() != null ? reg.getOperador().getNome() : "Desconhecido");
        dto.setMaquinaNome(reg.getMaquina() != null ? reg.getMaquina().getNome() : "Sem máquina");

        // Lógica de Geofencing para renomear a localização
        List<Cliente> matches = clienteRepository.encontrarClientePorCoordenada(reg.getLatitude(), reg.getLongitude());

        if (!matches.isEmpty()) {
            Cliente cliente = matches.get(0);
            dto.setClienteNome(cliente.getNome());
            dto.setLocalIdentificado(cliente.getLocalServico()); 
            dto.setIdentificadoPeloGps(true);
            dto.setValorTotal(reg.getHoras() * cliente.getValorHora());
        } else {
            dto.setClienteNome("Cliente Geral");
            dto.setLocalIdentificado("Localização não identificada");
            dto.setIdentificadoPeloGps(false);
            dto.setValorTotal(reg.getHoras() * 200.0); // Valor padrão de fallback
        }

        return dto;
    }
}