package com.kodarsoftwares.nivelar.config; // Ou o nome do seu pacote de configuração

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;

import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {

    private static final String TIME_FORMAT = "HH:mm"; // Define o formato desejado

    @Bean
    public JavaTimeModule javaTimeModule() {
        JavaTimeModule module = new JavaTimeModule();
        // Adiciona um serializador customizado APENAS para LocalTime
        module.addSerializer(java.time.LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(TIME_FORMAT)));
        // Os outros tipos de data/hora (LocalDate, LocalDateTime) continuarão com o formato padrão do Jackson
        return module;
    }

    // Opcional, mas bom para garantir consistência em outros lugares do Spring
    @Bean
    public FormattingConversionService conversionService() {
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService(false);

        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        // Registra o formato APENAS para LocalTime
        registrar.setTimeFormatter(DateTimeFormatter.ofPattern(TIME_FORMAT));
        // Se precisar formatar LocalDate ou LocalDateTime, adicione aqui:
        // registrar.setDateFormatter(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        // registrar.setDateTimeFormatter(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        registrar.registerFormatters(conversionService);

        return conversionService;
    }
}