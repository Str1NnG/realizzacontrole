package com.kodarsoftwares.nivelar.controller;

import com.kodarsoftwares.nivelar.dto.AnaliseRequestDTO;
import com.kodarsoftwares.nivelar.dto.FuncionarioDesempenhoDTO;
import com.kodarsoftwares.nivelar.dto.MaquinaDesempenhoDTO;
import com.kodarsoftwares.nivelar.repository.RegistroDiarioRepository;
import com.kodarsoftwares.nivelar.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final RegistroDiarioRepository registroDiarioRepository;
    private final PdfService pdfService;

    @PostMapping("/funcionarios")
    public ResponseEntity<List<FuncionarioDesempenhoDTO>> getDesempenhoFuncionarios(@RequestBody AnaliseRequestDTO request) {
        List<FuncionarioDesempenhoDTO> desempenho = registroDiarioRepository.findDesempenhoFuncionarios(
                request.getDataInicio(),
                request.getDataFim(),
                request.getDescricao()
        );
        return ResponseEntity.ok(desempenho);
    }

    @PostMapping("/maquinas")
    public ResponseEntity<List<MaquinaDesempenhoDTO>> getDesempenhoMaquinas(@RequestBody AnaliseRequestDTO request) {
        List<MaquinaDesempenhoDTO> desempenho = registroDiarioRepository.findDesempenhoMaquinas(
                request.getDataInicio(),
                request.getDataFim(),
                request.getDescricao()
        );
        return ResponseEntity.ok(desempenho);
    }

    @PostMapping("/pdf")
    public ResponseEntity<InputStreamResource> gerarPdf(@RequestBody AnaliseRequestDTO request) {
        List<FuncionarioDesempenhoDTO> desempenhoFuncionarios = registroDiarioRepository.findDesempenhoFuncionarios(
                request.getDataInicio(), request.getDataFim(), request.getDescricao());

        List<MaquinaDesempenhoDTO> desempenhoMaquinas = registroDiarioRepository.findDesempenhoMaquinas(
                request.getDataInicio(), request.getDataFim(), request.getDescricao());

        ByteArrayInputStream pdf = pdfService.gerarRelatorioPdf(request, desempenhoFuncionarios, desempenhoMaquinas);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=relatorio_nivelar.pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdf));
    }
}