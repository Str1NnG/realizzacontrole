package com.kodarsoftwares.nivelar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Aponta para a pasta real onde as fotos estão (uploads/registros)
    @Value("${nivelar.upload-dir.anexos:uploads/registros}")
    private String uploadDirAnexos;

    @Value("${nivelar.upload-dir.profile-pics:uploads/profile-pics}")
    private String uploadDirProfilePics;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Rota interna (sem o /api, pois o Tomcat já corta isso na entrada)
        exposeDirectory(uploadDirAnexos, "/files/anexos/**", registry);
        exposeDirectory(uploadDirProfilePics, "/files/profile-pics/**", registry);
        
        // Rota extra (com o /api) como garantia absoluta
        exposeDirectory(uploadDirAnexos, "/api/files/anexos/**", registry);
        exposeDirectory(uploadDirProfilePics, "/api/files/profile-pics/**", registry);
    }

    private void exposeDirectory(String dirName, String pathPattern, ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(dirName);
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        if (dirName.startsWith("../")) {
            dirName = dirName.replace("../", "");
        }

        registry.addResourceHandler(pathPattern)
                .addResourceLocations("file:/" + uploadPath + "/");
    }
}