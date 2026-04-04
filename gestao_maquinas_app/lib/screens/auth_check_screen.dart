import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:gestao_maquinas_app/constants/app_colors.dart'; // Importe as cores
import 'package:gestao_maquinas_app/services/api_service.dart';
import 'package:gestao_maquinas_app/services/auth_service.dart';

class AuthCheckScreen extends StatefulWidget {
  const AuthCheckScreen({super.key});

  @override
  State<AuthCheckScreen> createState() => _AuthCheckScreenState();
}

class _AuthCheckScreenState extends State<AuthCheckScreen> {
  final _storage = const FlutterSecureStorage();
  final _apiService = ApiService();
  final AuthService _authService = AuthService();

  @override
  void initState() {
    super.initState();
    _checkAuthStatus();
  }

  Future<void> _checkAuthStatus() async {
    // Adiciona um pequeno delay para a UI de loading ser visível
    await Future.delayed(const Duration(milliseconds: 500));

    final token = await _storage.read(key: 'jwt_token');

    if (token == null || token.isEmpty) {
      if (!mounted) return;
      _authService.clearUser();
      Navigator.pushReplacementNamed(context, '/login');
      return;
    }

    final cpf = await _storage.read(key: 'user_cpf_for_reload');
    if (cpf == null || cpf.isEmpty) {
      if (!mounted) return;
      _authService.clearUser();
      await _storage.deleteAll();
      Navigator.pushReplacementNamed(context, '/login');
      return;
    }

    try {
      final responseData = await _apiService.login(cpf);
      final userData = responseData['operador'] as Map<String, dynamic>;

      if (!mounted) return;
      _authService.setUser(userData);
      Navigator.pushReplacementNamed(context, '/list');
    } catch (e) {
      if (!mounted) return;
      _authService.clearUser();
      await _storage.deleteAll();
      Navigator.pushReplacementNamed(context, '/login');
    }
  }

  @override
  Widget build(BuildContext context) {
    // --- Tela de carregamento ATUALIZADA ---
    return const Scaffold(
      backgroundColor: AppColors.lightGrey, // Fundo claro
      body: Center(
        child: CircularProgressIndicator(
          valueColor: AlwaysStoppedAnimation<Color>(
            AppColors.primaryBlue,
          ), // Cor primária
        ),
      ),
    );
    // ------------------------------------
  }
}
