package com.kodarsoftwares.nivelar.service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.kodarsoftwares.nivelar.dto.AnaliseRequestDTO;
import com.kodarsoftwares.nivelar.dto.FuncionarioDesempenhoDTO;
import com.kodarsoftwares.nivelar.dto.MaquinaDesempenhoDTO;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfService {

    public ByteArrayInputStream gerarRelatorioPdf(
            AnaliseRequestDTO filtros,
            List<FuncionarioDesempenhoDTO> desempenhoFuncionarios,
            List<MaquinaDesempenhoDTO> desempenhoMaquinas) {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (PdfWriter writer = new PdfWriter(out);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            // --- PALETA DE CORES (Inspirada no Painel) ---
            DeviceRgb primaryDark = new DeviceRgb(15, 23, 42);    // bg-slate-900 (Azul muito escuro)
            DeviceRgb primaryBlue = new DeviceRgb(37, 99, 235);   // text-blue-600
            DeviceRgb lightGray = new DeviceRgb(248, 250, 252);   // bg-slate-50
            DeviceRgb borderColor = new DeviceRgb(226, 232, 240); // border-slate-200
            DeviceRgb textGray = new DeviceRgb(100, 116, 139);    // text-slate-500

            // --- CABEÇALHO COM LOGO E TÍTULO ---
            Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1, 2}));
            headerTable.setWidth(UnitValue.createPercentValue(100));
            headerTable.setMarginBottom(20);

            // Célula 1: Logomarca
            Cell logoCell = new Cell().setBorder(Border.NO_BORDER).setVerticalAlignment(VerticalAlignment.MIDDLE);
            try {
                // O Java vai procurar esse arquivo na pasta raiz do seu projeto (onde fica o pom.xml)
                ImageData imageData = ImageDataFactory.create("logo_realizza.png");
                Image logo = new Image(imageData).scaleToFit(130, 65);
                logoCell.add(logo);
            } catch (Exception e) {
                // Fallback de segurança: Se não achar a imagem, escreve o nome bonito
                logoCell.add(new Paragraph("REALIZZA\nENGENHARIA")
                        .setBold().setFontColor(primaryBlue).setFontSize(16));
            }
            headerTable.addCell(logoCell);

            // Célula 2: Informações do Relatório (Alinhado à Direita)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String periodo = filtros.getDataInicio().format(formatter) + " a " + filtros.getDataFim().format(formatter);

            Cell infoCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
            infoCell.add(new Paragraph("RELATÓRIO DE PRODUÇÃO").setFontColor(primaryDark).setBold().setFontSize(18).setMarginBottom(0));
            infoCell.add(new Paragraph("Resumo Operacional e Desempenho da Frota").setFontColor(textGray).setFontSize(10).setMarginBottom(5));
            infoCell.add(new Paragraph("Período: " + periodo).setBold().setFontSize(10).setFontColor(primaryDark));

            if (filtros.getDescricao() != null && !filtros.getDescricao().isEmpty()) {
                infoCell.add(new Paragraph("Filtro Aplicado: " + filtros.getDescricao()).setFontSize(9).setFontColor(textGray).setItalic());
            }
            headerTable.addCell(infoCell);
            
            document.add(headerTable);

            // Linha separadora elegante
            document.add(new Paragraph("").setBorderBottom(new SolidBorder(borderColor, 1)).setMarginBottom(25));

            // --- TABELA 1: DESEMPENHO DA EQUIPE ---
            document.add(new Paragraph("DESEMPENHO DA EQUIPE (OPERADORES)")
                    .setFontColor(primaryDark).setBold().setFontSize(12).setMarginBottom(10));

            Table tabelaFuncionarios = criarTabelaPadrao(primaryDark, borderColor, "Nome do Operador");

            double totalHorasFunc = 0;
            for (FuncionarioDesempenhoDTO dto : desempenhoFuncionarios) {
                adicionarLinhaTabela(tabelaFuncionarios, dto.getNomeFuncionario(), dto.getTotalHoras(), borderColor);
                totalHorasFunc += dto.getTotalHoras();
            }
            // Adiciona a linha de Soma Total embaixo
            adicionarLinhaTotal(tabelaFuncionarios, totalHorasFunc, primaryDark);

            document.add(tabelaFuncionarios);
            document.add(new Paragraph("\n")); // Espaçamento entre tabelas

            // --- TABELA 2: DESEMPENHO DAS MÁQUINAS ---
            document.add(new Paragraph("DESEMPENHO DA FROTA (EQUIPAMENTOS)")
                    .setFontColor(primaryDark).setBold().setFontSize(12).setMarginBottom(10));

            Table tabelaMaquinas = criarTabelaPadrao(primaryDark, borderColor, "Máquina / Equipamento");

            double totalHorasMaq = 0;
            for (MaquinaDesempenhoDTO dto : desempenhoMaquinas) {
                adicionarLinhaTabela(tabelaMaquinas, dto.getNomeMaquina(), dto.getTotalHoras(), borderColor);
                totalHorasMaq += dto.getTotalHoras();
            }
            // Adiciona a linha de Soma Total embaixo
            adicionarLinhaTotal(tabelaMaquinas, totalHorasMaq, primaryDark);

            document.add(tabelaMaquinas);

            // --- RODAPÉ ---
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
            Paragraph footer = new Paragraph("Relatório gerado automaticamente pelo sistema Realizza Controle em " + LocalDateTime.now().format(timeFormatter))
                    .setFontSize(8)
                    .setFontColor(textGray)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(40);
            document.add(footer);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // =========================================================
    // MÉTODOS AUXILIARES PARA MANTER O CÓDIGO LIMPO E BONITO
    // =========================================================

    private Table criarTabelaPadrao(DeviceRgb headerBg, DeviceRgb borderColor, String tituloColuna1) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{4, 1}));
        table.setWidth(UnitValue.createPercentValue(100));

        // Cabeçalho da Coluna 1
        Cell header1 = new Cell().add(new Paragraph(tituloColuna1).setBold().setFontSize(10))
                .setBackgroundColor(headerBg).setFontColor(ColorConstants.WHITE)
                .setBorder(Border.NO_BORDER).setPadding(8);

        // Cabeçalho da Coluna 2
        Cell header2 = new Cell().add(new Paragraph("Horas Produtivas").setBold().setFontSize(10))
                .setBackgroundColor(headerBg).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(Border.NO_BORDER).setPadding(8);

        table.addHeaderCell(header1);
        table.addHeaderCell(header2);

        return table;
    }

    private void adicionarLinhaTabela(Table table, String nome, double horas, DeviceRgb borderColor) {
        Cell cell1 = new Cell().add(new Paragraph(nome != null ? nome : "Não Informado").setFontSize(10))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(borderColor, 1)) // Borda apenas embaixo
                .setPadding(8);

        Cell cell2 = new Cell().add(new Paragraph(String.format("%.1f h", horas)).setFontSize(10).setBold())
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(borderColor, 1)) // Borda apenas embaixo
                .setPadding(8);

        table.addCell(cell1);
        table.addCell(cell2);
    }

    private void adicionarLinhaTotal(Table table, double totalHoras, DeviceRgb primaryDark) {
        Cell cellTotalText = new Cell().add(new Paragraph("TOTAL GERAL").setBold().setFontSize(10))
                .setBorder(Border.NO_BORDER)
                .setPadding(8)
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontColor(primaryDark);

        Cell cellTotalValue = new Cell().add(new Paragraph(String.format("%.1f h", totalHoras)).setBold().setFontSize(12))
                .setBorder(Border.NO_BORDER)
                .setPadding(8)
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontColor(primaryDark);

        table.addCell(cellTotalText);
        table.addCell(cellTotalValue);
    }
}