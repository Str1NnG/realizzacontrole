// lib/constants/app_colors.dart
import 'package:flutter/material.dart';

class AppColors {
  // --- PALETA REALIZZA OFICIAL (DOURADO) ---
  static const Color primaryRealizza = Color(0xFFC59344); // Dourado do Logo
  static const Color primaryDarkRealizza = Color(0xFFA67C39);

  static const Color white = Color(0xFFFFFFFF);
  static const Color lightGrey = Color(0xFFF8F8F8);
  static const Color textDark = Color(0xFF2D2D2D);
  static const Color textLightGrey = Color(0xFF8E8E8E);

  static const Color successGreen = Color(0xFF2E7D32);
  static const Color warningOrange = Color(0xFFFFA000);
  static const Color errorRed = Color(0xFFD32F2F);

  // --- COMPATIBILIDADE ---
  static const Color primaryBlue = primaryRealizza; // Agora é Dourado!
  static const Color textLight = white;
  static const Color darkGrey = textLightGrey;
  static const Color accentPurple = primaryDarkRealizza;
  static const Color goldenrod = primaryRealizza;
}
