import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:image_picker/image_picker.dart';
import 'package:intl/intl.dart';
import 'package:google_mlkit_text_recognition/google_mlkit_text_recognition.dart';
import 'package:geolocator/geolocator.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:gestao_maquinas_app/constants/app_colors.dart';
import '../services/api_service.dart';

class ReportFormScreen extends StatefulWidget {
  const ReportFormScreen({super.key});

  @override
  State<ReportFormScreen> createState() => _ReportFormScreenState();
}

class _ReportFormScreenState extends State<ReportFormScreen> {
  final _formKey = GlobalKey<FormState>();
  final _apiService = ApiService();
  final _storage = const FlutterSecureStorage();

  bool _isLoading = false;
  bool _isProcessingOCR = false;
  bool _isClosing = false;

  int _operadorId = -1;
  int? _registroId;

  Uint8List? _fotoBytes;
  String? _fotoFileName;
  Position? _currentPosition;

  final _horimetroController = TextEditingController();

  final ImagePicker _picker = ImagePicker();
  final textRecognizer = TextRecognizer(script: TextRecognitionScript.latin);

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final args =
        ModalRoute.of(context)?.settings.arguments as Map<String, dynamic>?;
    if (args != null) {
      _operadorId = args['operadorId'] ?? -1;
      _isClosing = args['isClosing'] ?? false;
      _registroId = args['registroId'];
    }
  }

  @override
  void dispose() {
    _horimetroController.dispose();
    textRecognizer.close();
    super.dispose();
  }

  Future<bool> _getLocation() async {
    bool serviceEnabled;
    LocationPermission permission;

    serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      _mostrarErro('Ligue o GPS do celular.');
      return false;
    }

    permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
      if (permission == LocationPermission.denied) {
        _mostrarErro('O aplicativo precisa do GPS para funcionar.');
        return false;
      }
    }

    if (permission == LocationPermission.deniedForever) {
      _mostrarErro('Vá nas configurações do celular e libere o GPS.');
      return false;
    }

    try {
      // TENTA PEGAR A LOCALIZAÇÃO EXATA (ESPERA NO MÁXIMO 5 SEGUNDOS)
      _currentPosition = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
        timeLimit: const Duration(
          seconds: 5,
        ), // <-- A MÁGICA QUE DESTRAVA A TELA
      );
    } catch (e) {
      // SE DEMORAR MAIS DE 5 SEGUNDOS (SEM INTERNET), PEGA A ÚLTIMA POSIÇÃO CONHECIDA E SEGUE O JOGO
      _currentPosition = await Geolocator.getLastKnownPosition();
    }

    return true;
  }

  void _mostrarErro(String msg) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(msg), backgroundColor: AppColors.errorRed),
    );
  }

  Future<void> _runOCR(XFile pickedFile) async {
    setState(() => _isProcessingOCR = true);
    try {
      final inputImage = InputImage.fromFilePath(pickedFile.path);
      final RecognizedText recognizedText = await textRecognizer.processImage(
        inputImage,
      );

      String? parteInteira;
      Rect? boxInteira;
      String? parteDecimal;

      for (TextBlock block in recognizedText.blocks) {
        for (TextLine line in block.lines) {
          String text = line.text.replaceAll(' ', '').replaceAll('.', ',');
          if (text.contains('/') || text.contains(':')) continue;

          if (RegExp(r'\d+[.,]\d').hasMatch(text)) {
            setState(() {
              _horimetroController.text = text;
            });
            // Pequeno delay antes de sumir o loading para evitar o erro de Buffer
            await Future.delayed(const Duration(milliseconds: 300));
            setState(() => _isProcessingOCR = false);
            return;
          }

          if (RegExp(r'^\d{4,6}$').hasMatch(text)) {
            parteInteira = text;
            boxInteira = line.boundingBox;
          }

          if (RegExp(r'^\d$').hasMatch(text)) {
            if (boxInteira != null) {
              final boxAtual = line.boundingBox;
              if (boxAtual.left > boxInteira.right &&
                  (boxAtual.top - boxInteira.top).abs() < 60) {
                parteDecimal = text;
              }
            } else {
              parteDecimal = text;
            }
          }
        }
      }

      if (parteInteira != null) {
        String valorFinal = (parteDecimal != null)
            ? "$parteInteira,$parteDecimal"
            : parteInteira;
        setState(() {
          _horimetroController.text = valorFinal;
        });
      }
    } catch (e) {
      debugPrint("Erro no leitor de foto: $e");
    } finally {
      // ESSENCIAL: Delay de meio segundo para o sistema gráfico limpar os buffers
      await Future.delayed(const Duration(milliseconds: 500));
      if (mounted) setState(() => _isProcessingOCR = false);
    }
  }

  Future<void> _pickImage() async {
    try {
      final XFile? pickedFile = await _picker.pickImage(
        source: ImageSource.camera,
        imageQuality: 50, // <-- VOLTAMOS PARA 50% (Nitidez garantida)
        maxWidth: 1024, // <-- RESOLUÇÃO OTIMIZADA (Não trava o buffer)
        maxHeight: 1024,
      );
      if (pickedFile != null) {
        final bytes = await pickedFile.readAsBytes();
        setState(() {
          _fotoBytes = bytes;
          _fotoFileName = pickedFile.name;
        });
        await _runOCR(pickedFile);
      }
    } catch (e) {
      _mostrarErro("Erro ao processar imagem. Libere memória.");
    }
  }

  Future<void> _submitForm() async {
    if (!_formKey.currentState!.validate() || _fotoBytes == null) {
      _mostrarErro('Tire a foto do painel para continuar.');
      return;
    }

    setState(() => _isLoading = true);

    try {
      await _getLocation();

      double? valorLido;
      String textoDigitado = _horimetroController.text.trim();

      if (textoDigitado.isNotEmpty) {
        String textoFormatado = textoDigitado.replaceAll(',', '.');
        valorLido = double.tryParse(textoFormatado);
      }

      if (!_isClosing) {
        // ==========================================
        // INICIANDO O SERVIÇO (GUARDA O VALOR INICIAL)
        // ==========================================
        double horimetroEnvio = valorLido ?? 0.0;

        final registroData = {
          'data': DateFormat('yyyy-MM-dd').format(DateTime.now()),
          'horimetroInicial': horimetroEnvio,
          'horimetroFinal': null,
          'latitude': _currentPosition?.latitude,
          'longitude': _currentPosition?.longitude,
          'operadorId': _operadorId,
        };

        await _apiService.createRegistro(
          registroData,
          _fotoBytes!,
          _fotoFileName!,
        );

        // Salva a hora exata e o valor do painel para o cálculo final
        await _storage.write(
          key: 'cronometro_inicio_$_operadorId',
          value: DateTime.now().toIso8601String(),
        );
        await _storage.write(
          key: 'horimetro_inicial_$_operadorId',
          value: horimetroEnvio.toString(),
        );
      } else {
        // ==========================================
        // FECHANDO O SERVIÇO (SOMA TEMPO + INICIAL)
        // ==========================================
        double? horimetroEnvioFinal = valorLido;

        if (horimetroEnvioFinal == null) {
          String? startTimeStr = await _storage.read(
            key: 'cronometro_inicio_$_operadorId',
          );
          String? hInicialStr = await _storage.read(
            key: 'horimetro_inicial_$_operadorId',
          );

          double hInicial = hInicialStr != null
              ? double.parse(hInicialStr)
              : 0.0;

          if (startTimeStr != null) {
            DateTime startTime = DateTime.parse(startTimeStr);
            Duration worked = DateTime.now().difference(startTime);

            double horasTrabalhadas = worked.inMinutes / 60.0;

            // TRATAMENTO ANTI-BUG (Testes rápidos não podem zerar)
            if (horasTrabalhadas <= 0) horasTrabalhadas = 0.01;

            horimetroEnvioFinal = double.parse(
              (hInicial + horasTrabalhadas).toStringAsFixed(2),
            );
          } else {
            horimetroEnvioFinal = hInicial + 0.1;
          }
        }

        final updateData = {
          'horimetroFinal': horimetroEnvioFinal,
          'latitude': _currentPosition?.latitude,
          'longitude': _currentPosition?.longitude,
        };

        await _apiService.updateRegistro(_registroId!, updateData);
        await _apiService.uploadAnexoFinal(
          _registroId!,
          _fotoBytes!,
          _fotoFileName!,
        );

        // Serviço encerrado! Limpa a memória do celular
        await _storage.delete(key: 'cronometro_inicio_$_operadorId');
        await _storage.delete(key: 'horimetro_inicial_$_operadorId');
      }

      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Tudo certo! Salvo com sucesso.'),
          backgroundColor: AppColors.successGreen,
        ),
      );
      Navigator.of(context).pop(true);
    } catch (e) {
      _mostrarErro("Erro ao salvar: verifique sua conexão.");
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final Color themeColor = _isClosing
        ? AppColors.errorRed
        : AppColors.successGreen;
    final String titleText = _isClosing
        ? "FINALIZAR SERVIÇO"
        : "INICIAR SERVIÇO";

    return Scaffold(
      backgroundColor: AppColors.white,
      appBar: AppBar(
        title: Text(
          titleText,
          style: GoogleFonts.poppins(fontWeight: FontWeight.w900),
        ),
        centerTitle: true,
        backgroundColor: themeColor,
        foregroundColor: Colors.white,
        elevation: 0,
      ),
      body: Form(
        key: _formKey,
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                "1. FOTO DO PAINEL",
                style: GoogleFonts.poppins(
                  fontSize: 22,
                  fontWeight: FontWeight.bold,
                  color: themeColor,
                ),
              ),
              const SizedBox(height: 15),

              if (_fotoBytes != null)
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 4,
                    vertical: 8,
                  ),
                  margin: const EdgeInsets.only(bottom: 10),
                  decoration: BoxDecoration(
                    color: AppColors.lightGrey,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    children: [
                      Expanded(
                        child: TextButton.icon(
                          onPressed: _isLoading ? null : _pickImage,
                          icon: Icon(
                            Icons.camera_alt,
                            color: themeColor,
                            size: 20,
                          ),
                          label: FittedBox(
                            fit: BoxFit.scaleDown,
                            child: Text(
                              "TIRAR OUTRA",
                              style: GoogleFonts.poppins(
                                color: themeColor,
                                fontWeight: FontWeight.bold,
                                fontSize: 14,
                              ),
                            ),
                          ),
                        ),
                      ),
                      Container(
                        width: 1,
                        height: 30,
                        color: Colors.grey.withOpacity(0.3),
                      ),
                      Expanded(
                        child: TextButton.icon(
                          onPressed: _isLoading
                              ? null
                              : () => setState(() {
                                  _fotoBytes = null;
                                  _horimetroController.clear();
                                }),
                          icon: const Icon(
                            Icons.delete_forever,
                            color: AppColors.errorRed,
                            size: 20,
                          ),
                          label: FittedBox(
                            fit: BoxFit.scaleDown,
                            child: Text(
                              "APAGAR FOTO",
                              style: GoogleFonts.poppins(
                                color: AppColors.errorRed,
                                fontWeight: FontWeight.bold,
                                fontSize: 14,
                              ),
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),

              GestureDetector(
                onTap: _fotoBytes == null ? _pickImage : null,
                child: Container(
                  height: 250,
                  decoration: BoxDecoration(
                    color: _fotoBytes == null
                        ? themeColor.withOpacity(0.1)
                        : Colors.black,
                    borderRadius: BorderRadius.circular(24),
                    border: Border.all(color: themeColor, width: 3),
                  ),
                  child: _fotoBytes == null
                      ? Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(Icons.camera_alt, size: 80, color: themeColor),
                            const SizedBox(height: 12),
                            Text(
                              "TOCAR PARA ABRIR A CÂMERA",
                              style: GoogleFonts.poppins(
                                fontSize: 16,
                                fontWeight: FontWeight.w900,
                                color: themeColor,
                              ),
                            ),
                          ],
                        )
                      : ClipRRect(
                          borderRadius: BorderRadius.circular(21),
                          child: Stack(
                            fit: StackFit.expand,
                            children: [
                              Image.memory(_fotoBytes!, fit: BoxFit.cover),
                              if (_isProcessingOCR)
                                Container(
                                  color: Colors.black54,
                                  child: Center(
                                    child: CircularProgressIndicator(
                                      color: themeColor,
                                    ),
                                  ),
                                ),
                            ],
                          ),
                        ),
                ),
              ),

              const SizedBox(height: 40),

              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    "2. NÚMERO (Opcional):",
                    style: GoogleFonts.poppins(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                      color: themeColor,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),

              TextFormField(
                controller: _horimetroController,
                keyboardType: const TextInputType.numberWithOptions(
                  decimal: true,
                ),
                inputFormatters: [
                  FilteringTextInputFormatter.allow(RegExp(r'^\d*[,.]?\d*$')),
                ],
                textAlign: TextAlign.center,
                style: GoogleFonts.poppins(
                  fontSize: 45,
                  fontWeight: FontWeight.w900,
                ),
                decoration: InputDecoration(
                  filled: true,
                  fillColor: AppColors.lightGrey,
                  hintText: '0,0',
                  hintStyle: TextStyle(color: Colors.grey.withOpacity(0.5)),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(16),
                    borderSide: BorderSide.none,
                  ),
                ),
                validator: (v) {
                  if (v == null || v.trim().isEmpty) return null;
                  if (double.tryParse(v.replaceAll(',', '.')) == null)
                    return 'Número inválido. Use formato como 1234,5';
                  return null;
                },
              ),

              const SizedBox(height: 48),

              ElevatedButton(
                style: ElevatedButton.styleFrom(
                  backgroundColor: themeColor,
                  padding: const EdgeInsets.symmetric(vertical: 24),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(24),
                  ),
                ),
                onPressed: _isLoading || _isProcessingOCR ? null : _submitForm,
                child: _isLoading
                    ? const CircularProgressIndicator(color: Colors.white)
                    : Text(
                        'SALVAR E ENVIAR',
                        style: GoogleFonts.poppins(
                          color: Colors.white,
                          fontWeight: FontWeight.w900,
                          fontSize: 24,
                        ),
                      ),
              ),
              const SizedBox(height: 20),
            ],
          ),
        ),
      ),
    );
  }
}
