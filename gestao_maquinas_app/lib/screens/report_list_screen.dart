// lib/screens/report_list_screen.dart

import 'dart:async';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:intl/intl.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:gestao_maquinas_app/constants/app_colors.dart';
import '../models/registro_diario.dart';
import '../services/api_service.dart';
import '../services/auth_service.dart';
import '../widgets/app_drawer.dart';

class ReportListScreen extends StatefulWidget {
  const ReportListScreen({super.key});

  @override
  State<ReportListScreen> createState() => _ReportListScreenState();
}

class _ReportListScreenState extends State<ReportListScreen> {
  final ApiService _apiService = ApiService();
  final AuthService _authService = AuthService();
  final _storage = const FlutterSecureStorage();

  bool _isLoading = true;
  bool _hasOpenService = false;
  RegistroDiario? _openRegistro;
  int? operadorId;

  // Variáveis do Cronómetro Visual
  Timer? _timer;
  Duration _elapsedTime = Duration.zero;

  @override
  void initState() {
    super.initState();
    final currentUser = _authService.currentUser;
    if (currentUser != null) {
      operadorId = currentUser.id;
      _checkMachineStatus();
    }
  }

  @override
  void dispose() {
    _timer?.cancel(); // Limpa o cronómetro da memória quando fechar o ecrã
    super.dispose();
  }

  void _startTimer(DateTime startTime) {
    _timer?.cancel();
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      setState(() {
        _elapsedTime = DateTime.now().difference(startTime);
      });
    });
  }

  Future<void> _checkMachineStatus() async {
    if (operadorId == null) return;
    setState(() => _isLoading = true);

    try {
      final registros = await _apiService.getRegistrosPorOperador(operadorId!);
      final hojeStr = DateFormat('yyyy-MM-dd').format(DateTime.now());

      final servicosAbertos = registros
          .where(
            (r) =>
                r.data == hojeStr &&
                (r.horimetroFinal == null || r.horimetroFinal == 0.0),
          )
          .toList();

      if (servicosAbertos.isNotEmpty) {
        _hasOpenService = true;
        _openRegistro = servicosAbertos.first;

        // Tenta ir buscar a hora em que o serviço começou
        String? startTimeStr = await _storage.read(
          key: 'cronometro_inicio_${operadorId!}',
        );
        if (startTimeStr != null) {
          DateTime startTime = DateTime.parse(startTimeStr);
          _startTimer(startTime);
        }
      } else {
        _hasOpenService = false;
        _openRegistro = null;
        _timer?.cancel();
        _elapsedTime = Duration.zero;
      }
    } catch (e) {
      debugPrint("Erro ao checar status: $e");
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _handleGiantButton() async {
    if (operadorId == null) return;

    final result = await Navigator.pushNamed(
      context,
      '/form',
      arguments: {
        'operadorId': operadorId!,
        'isClosing': _hasOpenService,
        'registroId': _openRegistro?.id,
      },
    );

    if (result == true && mounted) {
      _checkMachineStatus();
    }
  }

  // Formata o tempo para aparecer como 01:25:03
  String get _formattedTime {
    String hours = _elapsedTime.inHours.toString().padLeft(2, '0');
    String minutes = (_elapsedTime.inMinutes % 60).toString().padLeft(2, '0');
    String seconds = (_elapsedTime.inSeconds % 60).toString().padLeft(2, '0');
    return "$hours:$minutes:$seconds";
  }

  @override
  Widget build(BuildContext context) {
    final Color buttonColor = _hasOpenService
        ? AppColors.errorRed
        : AppColors.successGreen;
    final String buttonText = _hasOpenService
        ? "FINALIZAR\nSERVIÇO"
        : "INICIAR\nSERVIÇO";
    final IconData buttonIcon = _hasOpenService
        ? Icons.stop_circle_outlined
        : Icons.play_circle_fill_outlined;
    final String statusText = _hasOpenService
        ? "Máquina em Operação..."
        : "Máquina Parada";

    return Scaffold(
      backgroundColor: AppColors.white,
      drawer: const AppDrawer(),
      appBar: AppBar(
        title: Text(
          'Realizza Controle',
          style: GoogleFonts.poppins(fontWeight: FontWeight.bold),
        ),
        centerTitle: true,
        backgroundColor: AppColors.primaryRealizza,
        foregroundColor: AppColors.white,
        elevation: 0,
      ),
      body: _isLoading
          ? const Center(
              child: CircularProgressIndicator(
                color: AppColors.primaryRealizza,
              ),
            )
          : SafeArea(
              child: Padding(
                padding: const EdgeInsets.all(24.0),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Text(
                      statusText,
                      textAlign: TextAlign.center,
                      style: GoogleFonts.poppins(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                        color: AppColors.textDark,
                      ),
                    ),

                    // O CRONÓMETRO APARECE AQUI SE ESTIVER A TRABALHAR!
                    if (_hasOpenService) ...[
                      const SizedBox(height: 5),
                      Text(
                        _formattedTime,
                        textAlign: TextAlign.center,
                        style: GoogleFonts.poppins(
                          fontSize: 42,
                          fontWeight: FontWeight.w900,
                          color: AppColors.errorRed,
                          letterSpacing: 2,
                        ),
                      ),
                    ],

                    const SizedBox(height: 10),
                    Text(
                      "Toque no botão gigante abaixo",
                      textAlign: TextAlign.center,
                      style: GoogleFonts.poppins(
                        fontSize: 18,
                        color: AppColors.textLightGrey,
                      ),
                    ),
                    const SizedBox(height: 40),

                    Expanded(
                      child: GestureDetector(
                        onTap: _handleGiantButton,
                        child: Container(
                          decoration: BoxDecoration(
                            color: buttonColor,
                            borderRadius: BorderRadius.circular(40),
                            boxShadow: [
                              BoxShadow(
                                color: buttonColor.withOpacity(0.4),
                                blurRadius: 20,
                                offset: const Offset(0, 10),
                              ),
                            ],
                          ),
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(buttonIcon, size: 120, color: Colors.white),
                              const SizedBox(height: 20),
                              Text(
                                buttonText,
                                textAlign: TextAlign.center,
                                style: GoogleFonts.poppins(
                                  fontSize: 38,
                                  fontWeight: FontWeight.w900,
                                  color: Colors.white,
                                  height: 1.2,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 40),
                  ],
                ),
              ),
            ),
    );
  }
}
