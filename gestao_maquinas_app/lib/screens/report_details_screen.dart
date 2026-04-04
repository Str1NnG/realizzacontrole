// lib/screens/report_details_screen.dart

import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:gestao_maquinas_app/constants/app_colors.dart';
import '../models/registro_diario.dart';
import '../services/api_service.dart';

class ReportDetailsScreen extends StatefulWidget {
  final RegistroDiario registro;
  const ReportDetailsScreen({super.key, required this.registro});

  @override
  State<ReportDetailsScreen> createState() => _ReportDetailsScreenState();
}

class _ReportDetailsScreenState extends State<ReportDetailsScreen> {
  final ApiService _apiService = ApiService();
  Future<Uint8List>? _imageFuture;

  @override
  void initState() {
    super.initState();
    _imageFuture = _apiService.getAnexoBytes(widget.registro.id!);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.white,
      appBar: AppBar(
        title: Text(
          'Detalhes do Envio',
          style: GoogleFonts.poppins(fontWeight: FontWeight.bold),
        ),
        backgroundColor: AppColors.primaryRealizza, // DOURADO DA REALIZZA
        foregroundColor: AppColors.white,
        centerTitle: true,
        elevation: 0,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Cartões de Informação do Valor e Data
            _buildInfoCard(
              'Data do Envio',
              widget.registro.data ?? 'N/A',
              Icons.calendar_today_outlined,
            ),
            const SizedBox(height: 16),
            _buildInfoCard(
              'Valor do Horímetro',
              widget.registro.horimetroFinal.toString(),
              Icons.timer_outlined,
            ),

            const SizedBox(height: 40),

            Text(
              'Fotografia do Painel',
              style: GoogleFonts.poppins(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: AppColors.textDark,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 16),

            // Container que vai receber os bytes da foto via FutureBuilder
            Container(
              height: 350,
              decoration: BoxDecoration(
                color: AppColors.lightGrey,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(
                  color: AppColors.primaryRealizza,
                  width: 2,
                ), // Borda Dourada
              ),
              clipBehavior: Clip.hardEdge,
              child: FutureBuilder<Uint8List>(
                future: _imageFuture,
                builder: (context, snapshot) {
                  // Enquanto faz o download
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(
                      child: CircularProgressIndicator(
                        color: AppColors.primaryRealizza,
                      ),
                    );
                  }
                  // Se houver um erro (ex: foto não enviada ou apagada)
                  else if (snapshot.hasError) {
                    return Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Icon(
                          Icons.broken_image_outlined,
                          size: 64,
                          color: AppColors.textLightGrey,
                        ),
                        const SizedBox(height: 12),
                        Text(
                          'Nenhuma foto encontrada\nou erro de conexão.',
                          style: GoogleFonts.poppins(
                            color: AppColors.textLightGrey,
                          ),
                          textAlign: TextAlign.center,
                        ),
                      ],
                    );
                  }
                  // Se os bytes chegarem com sucesso, desenha a imagem na tela
                  else if (snapshot.hasData) {
                    return Image.memory(snapshot.data!, fit: BoxFit.cover);
                  }
                  // Estado vazio por segurança
                  else {
                    return const SizedBox.shrink();
                  }
                },
              ),
            ),
            const SizedBox(height: 40),
          ],
        ),
      ),
    );
  }

  // Componente visual para os cartões
  Widget _buildInfoCard(String label, String value, IconData icon) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppColors.lightGrey,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        children: [
          Icon(icon, color: AppColors.primaryRealizza, size: 32),
          const SizedBox(width: 20),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                label,
                style: GoogleFonts.poppins(
                  fontSize: 14,
                  color: AppColors.textLightGrey,
                ),
              ),
              Text(
                value,
                style: GoogleFonts.poppins(
                  fontSize: 22,
                  fontWeight: FontWeight.bold,
                  color: AppColors.textDark,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
