import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:mask_text_input_formatter/mask_text_input_formatter.dart';
import 'package:gestao_maquinas_app/constants/app_colors.dart';
import '../services/api_service.dart';
import '../services/auth_service.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final ApiService _apiService = ApiService();
  final AuthService _authService = AuthService();
  final _storage = const FlutterSecureStorage();

  final TextEditingController _cpfController = TextEditingController();
  final MaskTextInputFormatter _cpfMask = MaskTextInputFormatter(
    mask: '###.###.###-##',
    filter: {"#": RegExp(r'[0-9]')},
  );

  bool _isLoading = false;

  Future<void> _fazerLogin() async {
    if (_cpfController.text.length < 14) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Por favor, digite o CPF completo.'),
          backgroundColor: AppColors.warningOrange,
        ),
      );
      return;
    }

    setState(() => _isLoading = true);

    try {
      final responseData = await _apiService.login(_cpfController.text);
      final token = responseData['token'] as String;
      final userData = responseData['operador'] as Map<String, dynamic>;
      final unmaskedCpf = _cpfController.text.replaceAll(RegExp(r'[.-]'), '');

      await _storage.write(key: 'jwt_token', value: token);
      await _storage.write(key: 'user_cpf_for_reload', value: unmaskedCpf);
      _authService.setUser(userData);

      if (!mounted) return;
      Navigator.pushReplacementNamed(context, '/list');
    } catch (error) {
      if (!mounted) return;
      _authService.clearUser();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(error.toString()),
          backgroundColor: AppColors.errorRed,
        ),
      );
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  void dispose() {
    _cpfController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.primaryRealizza,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 32.0),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // LOGO OFICIAL DA REALIZZA
                Container(
                  height: 140,
                  margin: const EdgeInsets.only(bottom: 24),
                  child: Image.asset(
                    'assets/images/logo_realizza.png',
                    fit: BoxFit.contain,
                    errorBuilder: (context, error, stackTrace) {
                      // Fallback visual caso a imagem ainda não tenha sido encontrada no pubspec
                      return const Icon(
                        Icons.image_not_supported,
                        size: 80,
                        color: Colors.white54,
                      );
                    },
                  ),
                ),

                Text(
                  'Realizza Controle',
                  textAlign: TextAlign.center,
                  style: GoogleFonts.poppins(
                    fontSize: 30,
                    fontWeight: FontWeight.bold,
                    color: AppColors.white,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  'Envio de Horímetro Diário',
                  textAlign: TextAlign.center,
                  style: GoogleFonts.poppins(
                    fontSize: 16,
                    color: AppColors.white.withOpacity(0.9),
                  ),
                ),
                const SizedBox(height: 64),

                TextFormField(
                  controller: _cpfController,
                  inputFormatters: [_cpfMask],
                  keyboardType: TextInputType.number,
                  style: GoogleFonts.poppins(
                    fontSize: 18,
                    color: AppColors.textDark,
                  ),
                  decoration: InputDecoration(
                    hintText: 'Digite seu CPF',
                    hintStyle: GoogleFonts.poppins(
                      color: AppColors.textLightGrey,
                    ),
                    prefixIcon: const Icon(
                      Icons.badge_outlined,
                      color: AppColors.primaryRealizza,
                    ),
                    filled: true,
                    fillColor: AppColors.white,
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(16),
                      borderSide: BorderSide.none,
                    ),
                  ),
                ),
                const SizedBox(height: 32),

                ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 18),
                    backgroundColor: AppColors.white,
                    foregroundColor: AppColors.primaryRealizza,
                    elevation: 3,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                  ),
                  onPressed: _isLoading ? null : _fazerLogin,
                  child: _isLoading
                      ? const SizedBox(
                          height: 24,
                          width: 24,
                          child: CircularProgressIndicator(
                            color: AppColors.primaryRealizza,
                            strokeWidth: 3,
                          ),
                        )
                      : Text(
                          'ACESSAR SISTEMA',
                          style: GoogleFonts.poppins(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
