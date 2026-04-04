package com.kodarsoftwares.nivelar.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${nivelar.upload-dir.registros}")
    private String uploadDirRegistros;

    private Path fileStorageLocation;

    @PostConstruct
    public void init() {
        try {
            this.fileStorageLocation = Paths.get(uploadDirRegistros).toAbsolutePath().normalize();

            // --- LOG DE INICIALIZAÇÃO ADICIONADO ---
            // Isso vai mostrar no console exatamente qual pasta está sendo usada
            System.out.println("-----------------------------------------------------------------");
            System.out.println("### FileStorageService: Diretório de upload de registros: " + this.fileStorageLocation.toString());
            System.out.println("-----------------------------------------------------------------");
            // --- FIM DO LOG ---

            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Não foi possível criar o diretório para salvar os arquivos.", ex);
        }
    }

    public FileStorageService() {
    }

    public String storeFile(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileName = "";

        try {
            String fileExtension = "";
            int i = originalFileName.lastIndexOf('.');
            if (i > 0) {
                fileExtension = originalFileName.substring(i);
            }

            fileName = UUID.randomUUID().toString() + fileExtension;

            if (fileName.contains("..")) {
                throw new RuntimeException("Desculpe! O nome do arquivo contém uma sequência de caminho inválida " + fileName);
            }

            Path targetLocation = this.fileStorageLocation.resolve(fileName);

            // --- LOG DE SALVAMENTO ADICIONADO ---
            System.out.println("### FileStorageService: Salvando arquivo [" + originalFileName + "] como [" + fileName + "] em: " + targetLocation.toString());
            // --- FIM DO LOG ---

            Files.copy(file.getInputStream(), targetLocation);

            return fileName;
        } catch (IOException ex) {
            // --- LOG DE ERRO ADICIONADO ---
            System.err.println("### FileStorageService: ERRO ao salvar " + fileName + ". Causa: " + ex.getMessage());
            // --- FIM DO LOG ---
            throw new RuntimeException("Não foi possível salvar o arquivo " + fileName + ". Por favor, tente novamente!", ex);
        }
    }

    /**
     * Deleta um arquivo físico do disco.
     * Retorna 'true' se o arquivo foi deletado ou se não existia.
     * Retorna 'false' se ocorreu um erro ao tentar deletar.
     */
    public boolean deleteFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return true; // Nada a fazer
        }
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();

            // --- LOG DE DELEÇÃO ADICIONADO ---
            if(Files.exists(filePath)) {
                System.out.println("### FileStorageService: Deletando arquivo: " + filePath.toString());
            }
            // --- FIM DO LOG ---

            return Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            System.err.println("### FileStorageService: ERRO ao deletar o arquivo: " + fileName + " - " + ex.getMessage());
            return false;
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Não foi possível ler o arquivo: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Não foi possível ler o arquivo: " + fileName, ex);
        }
    }
}
