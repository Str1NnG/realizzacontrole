package com.kodarsoftwares.nivelar.controller;

import com.kodarsoftwares.nivelar.dto.RegistroDiarioDTO;
import com.kodarsoftwares.nivelar.model.RegistroDiario;
import com.kodarsoftwares.nivelar.service.RegistroDiarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/registros")
public class RegistroDiarioController {

    @Autowired
    private RegistroDiarioService registroService;

    @GetMapping("/operador/{operadorId}")
    public ResponseEntity<List<RegistroDiarioDTO>> getRegistrosPorOperador(@PathVariable Long operadorId) {
        List<RegistroDiarioDTO> registros = registroService.findByOperadorId(operadorId);
        return ResponseEntity.ok(registros);
    }

    @GetMapping
    public ResponseEntity<List<RegistroDiarioDTO>> getAllRegistros() {
        List<RegistroDiarioDTO> registros = registroService.findAll();
        return ResponseEntity.ok(registros);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RegistroDiarioDTO> getRegistroById(@PathVariable Long id) {
        return registroService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<RegistroDiarioDTO> createRegistro(
            @RequestPart("registro") String registroJson, 
            @RequestPart(value = "anexo", required = false) MultipartFile anexo,
            @RequestPart(value = "anexoInicio", required = false) MultipartFile anexoInicio, 
            @RequestPart(value = "anexoFinal", required = false) MultipartFile anexoFinal     
    ) {
        try {
            RegistroDiarioDTO novoRegistro = registroService.create(registroJson, anexo, anexoInicio, anexoFinal); 
            return ResponseEntity.status(HttpStatus.CREATED).body(novoRegistro);
        } catch (Exception e) {
            System.err.println("❌ ERRO AO CRIAR REGISTRO: ");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/manual")
    public ResponseEntity<RegistroDiarioDTO> createRegistroManual(@RequestBody RegistroDiario registro) {
        try {
            RegistroDiarioDTO novoRegistro = registroService.createManual(registro);
            return ResponseEntity.status(HttpStatus.CREATED).body(novoRegistro);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<RegistroDiario> updateRegistro(@PathVariable Long id, @RequestBody RegistroDiario registroDetails) {
        
        // --- LOG PARA DEBUG (O QUE O JAVA RECEBEU?) ---
        System.out.println("\n=== TENTATIVA DE FINALIZAR SERVIÇO #" + id + " ===");
        System.out.println("Horímetro Final recebido: " + registroDetails.getHorimetroFinal());
        System.out.println("Latitude: " + registroDetails.getLatitude());
        System.out.println("Longitude: " + registroDetails.getLongitude());
        System.out.println("JSON Completo mapeado: " + registroDetails.toString());
        System.out.println("==================================================\n");

        try {
            return ResponseEntity.ok(registroService.update(id, registroDetails));
        } catch (Exception e) {
            System.err.println("❌ ERRO FATAL AO ATUALIZAR REGISTRO #" + id);
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- NOVO ENDPOINT FINANCEIRO ---
    @PutMapping("/pagar")
    public ResponseEntity<Void> marcarComoPago(@RequestBody List<Long> ids) {
        try {
            registroService.marcarComoPago(ids);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRegistro(@PathVariable Long id) {
        try {
            registroService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/anexo")
    public ResponseEntity<Void> handleFileUpload(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            registroService.saveAnexo(id, file);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/anexo-inicio")
    public ResponseEntity<Void> handleAnexoInicioUpload(@PathVariable Long id, @RequestParam("anexoInicio") MultipartFile file) {
        try {
            registroService.saveAnexoInicio(id, file);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/anexo-final")
    public ResponseEntity<Void> handleAnexoFinalUpload(@PathVariable Long id, @RequestParam("anexoFinal") MultipartFile file) {
        try {
            registroService.saveAnexoFinal(id, file);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/anexo")
    public ResponseEntity<byte[]> getAnexo(@PathVariable Long id) {
        return loadAnexoResource(id, "principal");
    }

    @GetMapping("/{id}/anexo-inicio")
    public ResponseEntity<byte[]> getAnexoInicio(@PathVariable Long id) {
        return loadAnexoResource(id, "inicio");
    }

    @GetMapping("/{id}/anexo-final")
    public ResponseEntity<byte[]> getAnexoFinal(@PathVariable Long id) {
        return loadAnexoResource(id, "final");
    }

    @DeleteMapping("/{id}/anexo")
    public ResponseEntity<Void> deleteAnexo(@PathVariable Long id) {
        try {
            registroService.deleteAnexo(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}/anexo-inicio")
    public ResponseEntity<Void> deleteAnexoInicio(@PathVariable Long id) {
        try {
            registroService.deleteAnexoInicio(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}/anexo-final")
    public ResponseEntity<Void> deleteAnexoFinal(@PathVariable Long id) {
        try {
            registroService.deleteAnexoFinal(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResponseEntity<byte[]> loadAnexoResource(Long id, String tipoAnexo) {
        try {
            Resource file = registroService.loadFileAsResource(id, tipoAnexo);
            String contentType = "application/octet-stream";
            try {
                contentType = Files.probeContentType(file.getFile().toPath());
            } catch (IOException e) {
                // Ignora se não conseguir adivinhar o tipo
            }
            byte[] data = StreamUtils.copyToByteArray(file.getInputStream());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(data);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }
}