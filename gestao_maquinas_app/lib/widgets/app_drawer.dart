// lib/widgets/app_drawer.dart

import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:gestao_maquinas_app/constants/app_colors.dart';
import '../services/auth_service.dart';

class AppDrawer extends StatefulWidget {
  const AppDrawer({super.key});

  @override
  State<AppDrawer> createState() => _AppDrawerState();
}

class _AppDrawerState extends State<AppDrawer> {
  final _storage = const FlutterSecureStorage();
  final AuthService _authService = AuthService();

  @override
  Widget build(BuildContext context) {
    return Drawer(
      backgroundColor: AppColors.white,
      child: Column(
        children: <Widget>[
          // Header Realizza
          UserAccountsDrawerHeader(
            decoration: const BoxDecoration(color: AppColors.primaryRealizza),
            currentAccountPicture: Container(
              padding: const EdgeInsets.all(8),
              decoration: const BoxDecoration(
                color: AppColors.white,
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.person,
                size: 50,
                color: AppColors.primaryRealizza,
              ),
            ),
            accountName: ValueListenableBuilder<UserData?>(
              valueListenable: _authService.userDataListenable,
              builder: (context, user, child) {
                return Text(
                  user?.nome ?? 'Operador Realizza',
                  style: GoogleFonts.poppins(
                    color: AppColors.white,
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                );
              },
            ),
            accountEmail: Text(
              'Realizza Controle',
              style: GoogleFonts.poppins(
                color: AppColors.white.withOpacity(0.8),
                fontSize: 14,
              ),
            ),
          ),

          // Itens do Menu
          _buildDrawerItem(
            icon: Icons.history,
            title: 'Histórico de Envios',
            onTap: () {
              Navigator.pop(context);
              if (ModalRoute.of(context)?.settings.name != '/list') {
                Navigator.pushReplacementNamed(context, '/list');
              }
            },
            context: context,
            isActive: ModalRoute.of(context)?.settings.name == '/list',
          ),

          const Spacer(), // Empurra o logout para o final
          const Divider(),

          _buildDrawerItem(
            icon: Icons.logout,
            title: 'Sair do App',
            onTap: () async {
              await _storage.deleteAll();
              _authService.clearUser();
              Navigator.of(
                context,
              ).pushNamedAndRemoveUntil('/login', (route) => false);
            },
            context: context,
            isLogout: true,
          ),
          const SizedBox(height: 16),
        ],
      ),
    );
  }

  Widget _buildDrawerItem({
    required IconData icon,
    required String title,
    required VoidCallback onTap,
    required BuildContext context,
    bool isActive = false,
    bool isLogout = false,
  }) {
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 24, vertical: 4),
      leading: Icon(
        icon,
        color: isLogout
            ? AppColors.errorRed
            : (isActive ? AppColors.primaryRealizza : AppColors.textLightGrey),
      ),
      title: Text(
        title,
        style: GoogleFonts.poppins(
          fontSize: 16,
          fontWeight: isActive ? FontWeight.w600 : FontWeight.normal,
          color: isLogout
              ? AppColors.errorRed
              : (isActive ? AppColors.primaryRealizza : AppColors.textDark),
        ),
      ),
      selected: isActive,
      selectedTileColor: AppColors.primaryRealizza.withOpacity(0.08),
      onTap: onTap,
    );
  }
}
