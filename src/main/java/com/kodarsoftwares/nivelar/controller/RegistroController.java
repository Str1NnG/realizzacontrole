package com.kodarsoftwares.nivelar.controller;

import com.kodarsoftwares.nivelar.dto.RegistroDTO;
import com.kodarsoftwares.nivelar.repository.RegistroRepository;
import com.kodarsoftwares.nivelar.service.RegistroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/registros")
public class RegistroController {

    @Autowired
    private RegistroRepository registroRepository;

    @Autowired
    private RegistroService registroService;

    @GetMapping("/financeiro")
    public List<RegistroDTO> getRegistrosParaRecibo() {
        return registroRepository.findAll().stream()
                .map(registroService::converterParaDTO)
                .collect(Collectors.toList());
    }
}