import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:gestao_maquinas_app/constants/app_colors.dart'; // Importe as cores
import 'package:gestao_maquinas_app/services/auth_service.dart';
import 'package:gestao_maquinas_app/widgets/app_drawer.dart'; // Importe o AppDrawer
import 'package:mask_text_input_formatter/mask_text_input_formatter.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  final AuthService _authService = AuthService();
  UserData? _currentUser;

  final _cpfMask = MaskTextInputFormatter(
    mask: '###.###.###-##',
    filter: {"#": RegExp(r'[0-9]')},
  );

  @override
  void initState() {
    super.initState();
    _currentUser = _authService.currentUser;
    _authService.userDataListenable.addListener(_updateUser);
  }

  @override
  void dispose() {
    _authService.userDataListenable.removeListener(_updateUser);
    super.dispose();
  }

  void _updateUser() {
    if (mounted) {
      setState(() {
        _currentUser = _authService.currentUser;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    // --- Cores e Estilos ATUALIZADOS ---
    final String displayName = _currentUser?.nome ?? 'Usuário';
    final String displayRole =
        _currentUser?.cargo?.replaceFirst('ROLE_', '').replaceAll('_', ' ') ??
        'Cargo não informado';
    final String displayCpf = _currentUser?.cpf != null
        ? _cpfMask.maskText(_currentUser!.cpf.replaceAll(RegExp(r'[.-]'), ''))
        : '---.---.---.--';
    const String displayEmail = 'E-mail não disponível';
    const String displayPhone = 'Telefone não disponível';

    return Scaffold(
      drawer: const AppDrawer(), // Adiciona o drawer aqui
      appBar: AppBar(
        title: const Text('Meu Perfil'),
        // Estilo já definido globalmente
      ),
      body: SingleChildScrollView(
        child: Column(
          children: [
            // --- Cabeçalho ATUALIZADO ---
            Container(
              padding: const EdgeInsets.symmetric(vertical: 32, horizontal: 16),
              width: double.infinity,
              decoration: const BoxDecoration(
                gradient: LinearGradient(
                  colors: [AppColors.primaryBlue, AppColors.accentPurple],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
              ),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  CircleAvatar(
                    radius: 55,
                    backgroundColor: AppColors.textLight.withOpacity(0.9),
                    child: Text(
                      displayName.isNotEmpty
                          ? displayName[0].toUpperCase()
                          : '?',
                      style: const TextStyle(
                        fontSize: 55,
                        color: AppColors.primaryBlue,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    displayName,
                    style: GoogleFonts.poppins(
                      fontSize: 22,
                      fontWeight: FontWeight.w600,
                      color: AppColors.textLight,
                    ),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 4),
                  Text(
                    displayRole,
                    style: GoogleFonts.poppins(
                      fontSize: 15,
                      color: AppColors.lightGrey.withOpacity(0.85),
                    ),
                    textAlign: TextAlign.center,
                  ),
                ],
              ),
            ),
            // --- Informações ATUALIZADAS ---
            Padding(
              padding: const EdgeInsets.all(16.0),
              child: Container(
                padding: const EdgeInsets.symmetric(vertical: 8.0),
                decoration: BoxDecoration(
                  color: AppColors.textLight,
                  borderRadius: BorderRadius.circular(12.0),
                  boxShadow: [
                    BoxShadow(
                      color: AppColors.darkGrey.withOpacity(0.08),
                      blurRadius: 10,
                      offset: const Offset(0, 2),
                    ),
                  ],
                ),
                child: Column(
                  children: [
                    _buildInfoTile(
                      icon: Icons.badge_outlined,
                      label: 'CPF',
                      value: displayCpf,
                    ),
                    const Divider(height: 1, indent: 16, endIndent: 16),
                    _buildInfoTile(
                      icon: Icons.email_outlined,
                      label: 'E-mail',
                      value: displayEmail,
                    ),
                    const Divider(height: 1, indent: 16, endIndent: 16),
                    _buildInfoTile(
                      icon: Icons.phone_outlined,
                      label: 'Telefone',
                      value: displayPhone,
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 20),
          ],
        ),
      ),
    );
    // ---------------------------------
  }

  // --- Widget _buildInfoTile ATUALIZADO ---
  Widget _buildInfoTile({
    required IconData icon,
    required String label,
    required String value,
  }) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 16.0),
      child: Row(
        children: [
          Icon(icon, color: AppColors.primaryBlue.withOpacity(0.8), size: 24),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: GoogleFonts.poppins(
                    fontSize: 13,
                    color: AppColors.darkGrey.withOpacity(0.6),
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  value,
                  style: GoogleFonts.poppins(
                    fontSize: 16,
                    fontWeight: FontWeight.w500,
                    color: AppColors.textDark,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // ------------------------------------
}
