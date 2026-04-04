import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:gestao_maquinas_app/constants/app_colors.dart'; // Importe as novas cores
import 'package:gestao_maquinas_app/screens/auth_check_screen.dart';
import 'package:gestao_maquinas_app/screens/login_screen.dart';
import 'package:gestao_maquinas_app/screens/profile_screen.dart';
import 'package:gestao_maquinas_app/screens/report_form_screen.dart';
import 'package:gestao_maquinas_app/screens/report_list_screen.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      // --- NOME DO APLICATIVO ---
      title: 'Nivelar',
      // --------------------------
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        primaryColor: AppColors.primaryBlue,
        colorScheme:
            ColorScheme.fromSwatch(
              primarySwatch: MaterialColor(AppColors.primaryBlue.value, {
                50: AppColors.primaryBlue.withOpacity(0.1),
                100: AppColors.primaryBlue.withOpacity(0.2),
                200: AppColors.primaryBlue.withOpacity(0.3),
                300: AppColors.primaryBlue.withOpacity(0.4),
                400: AppColors.primaryBlue.withOpacity(0.5),
                500: AppColors.primaryBlue,
                600: AppColors.primaryBlue.withOpacity(0.8),
                700: AppColors.primaryBlue.withOpacity(0.9),
                800: AppColors.primaryBlue.withOpacity(1.0),
                900: AppColors.primaryBlue.withOpacity(1.0),
              }),
              accentColor: AppColors.accentPurple,
              backgroundColor: AppColors.lightGrey,
              errorColor: AppColors.errorRed,
            ).copyWith(
              secondary: AppColors
                  .goldenrod, // Cor secundária para o dourado (accentColor está obsoleto)
            ),

        scaffoldBackgroundColor: AppColors.lightGrey,

        appBarTheme: AppBarTheme(
          backgroundColor: AppColors.primaryBlue,
          foregroundColor: AppColors.textLight,
          elevation: 0,
          titleTextStyle: GoogleFonts.poppins(
            fontSize: 20,
            fontWeight: FontWeight.w600,
            color: AppColors.textLight,
          ),
          iconTheme: const IconThemeData(color: AppColors.textLight),
        ),

        // --- CORREÇÃO AQUI ---
        cardTheme: CardThemeData(
          // Usar CardThemeData
          elevation: 1,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
          color: AppColors.textLight,
          margin: EdgeInsets.zero, // Remove margens padrão se necessário
        ),

        // --- FIM DA CORREÇÃO ---
        inputDecorationTheme: InputDecorationTheme(
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: BorderSide(color: AppColors.darkGrey.withOpacity(0.3)),
          ),
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: BorderSide(color: AppColors.darkGrey.withOpacity(0.2)),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(12),
            borderSide: const BorderSide(
              color: AppColors.primaryBlue,
              width: 2,
            ),
          ),
          labelStyle: GoogleFonts.poppins(
            color: AppColors.textDark.withOpacity(0.7),
          ),
          hintStyle: GoogleFonts.poppins(
            color: AppColors.darkGrey.withOpacity(0.5),
          ),
          fillColor: AppColors.textLight,
          filled: true,
          contentPadding: const EdgeInsets.symmetric(
            vertical: 16,
            horizontal: 16,
          ),
        ),

        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(
            backgroundColor: AppColors.primaryBlue,
            foregroundColor: AppColors.textLight,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
            padding: const EdgeInsets.symmetric(vertical: 16),
            elevation: 0,
            textStyle: GoogleFonts.poppins(
              fontSize: 16,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),

        textButtonTheme: TextButtonThemeData(
          style: TextButton.styleFrom(
            foregroundColor: AppColors.primaryBlue,
            textStyle: GoogleFonts.poppins(fontWeight: FontWeight.w500),
          ),
        ),

        floatingActionButtonTheme: const FloatingActionButtonThemeData(
          backgroundColor: AppColors.primaryBlue,
          foregroundColor: AppColors.textLight,
          elevation: 4,
        ),

        textTheme: GoogleFonts.poppinsTextTheme().copyWith(
          bodyLarge: GoogleFonts.poppins(color: AppColors.textDark),
          bodyMedium: GoogleFonts.poppins(
            color: AppColors.textDark.withOpacity(0.8),
          ),
          labelLarge: GoogleFonts.poppins(
            color: AppColors.textDark,
          ), // Usado por alguns botões
        ),
      ),
      initialRoute: '/',
      routes: {
        '/': (context) => const AuthCheckScreen(),
        '/login': (context) => const LoginScreen(),
        '/list': (context) => const ReportListScreen(),
        '/form': (context) => const ReportFormScreen(),
        '/profile': (context) => const ProfileScreen(),
      },
    );
  }
}
