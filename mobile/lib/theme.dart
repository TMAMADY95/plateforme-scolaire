import 'package:flutter/material.dart';

/// Nom de l'application, affiché dans les barres de titre et sur la connexion.
const String nomApplication = 'EduManage';
const String sousTitreApplication = 'Plateforme nationale de gestion scolaire';

/// Charte : bleu marine pour les barres et les en-têtes, bleu vif pour les actions, fond gris très clair.
const Color couleurMarine = Color(0xFF0A2A63);
const Color couleurBleu = Color(0xFF1F5BCC);
const Color couleurFond = Color(0xFFF3F6FB);
const Color couleurBordure = Color(0xFFE4E9F2);

ThemeData construireTheme() {
  final schema = ColorScheme.fromSeed(seedColor: couleurBleu, primary: couleurBleu, surface: Colors.white);
  final arrondi = RoundedRectangleBorder(borderRadius: BorderRadius.circular(12));
  return ThemeData(
    colorScheme: schema,
    useMaterial3: true,
    scaffoldBackgroundColor: couleurFond,
    appBarTheme: const AppBarTheme(
      backgroundColor: couleurMarine,
      foregroundColor: Colors.white,
      elevation: 0,
      scrolledUnderElevation: 0,
      centerTitle: false,
    ),
    cardTheme: CardThemeData(
      color: Colors.white,
      elevation: 0,
      margin: const EdgeInsets.symmetric(vertical: 5),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: const BorderSide(color: couleurBordure),
      ),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        minimumSize: const Size.fromHeight(48),
        shape: arrondi,
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(style: OutlinedButton.styleFrom(shape: arrondi)),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: Colors.white,
      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
    ),
    tabBarTheme: const TabBarThemeData(
      labelColor: Colors.white,
      unselectedLabelColor: Color(0xFFB9C9EA),
      indicatorColor: Colors.white,
    ),
  );
}
