package com.kodarsoftwares.nivelar.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/files")
public class FileController {

    // Aponta exatamente para a pasta que vimos no seu log anterior
    @Value("${nivelar.upload-dir.registros:C:/opt/nivelar/uploads/registros}")
    private String uploadDirAnexos;

    @GetMapping("/anexos/{filename:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        
        System.out.println("\n=== [DEBUG] REQUISIÇÃO DE IMAGEM RECEBIDA ===");
        System.out.println("1. Arquivo solicitado pelo Angular: " + filename);
        
        try {
            // Monta o caminho exato do Windows
            Path filePath = Paths.get(uploadDirAnexos).resolve(filename).normalize();
            System.out.println("2. Procurando no HD físico em: " + filePath.toAbsolutePath());
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                System.out.println("3. ✅ Arquivo encontrado! Fazendo streaming ultra-rápido para o Angular...");
                
                // Tenta descobrir se é jpg, png, etc.
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource); // Retorna direto como Stream, não trava a memória RAM!
            } else {
                System.out.println("3. ❌ Arquivo NÃO encontrado ou o Java não tem permissão para ler!");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.out.println("3. ❌ ERRO INTERNO AO LER O ARQUIVO:");
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        } finally {
            System.out.println("=============================================\n");
        }
    }
}