import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

/// Palette mobile : bleu ciel, texte encre.
/// Typographie "Plus Jakarta Sans" : géométrique moderne,
// fûtés resserrés sur les titres.
const desktopAccent = Color(0xFF0EA5E9);
const desktopInk = Color(0xFF101828);

final _baseText = GoogleFonts.plusJakartaSansTextTheme();

TextTheme _tight(TextTheme t) => t.copyWith(
      displayLarge: t.displayLarge?.copyWith(letterSpacing: -1.5, fontWeight: FontWeight.w800),
      displayMedium: t.displayMedium?.copyWith(letterSpacing: -1.0, fontWeight: FontWeight.w800),
      displaySmall: t.displaySmall?.copyWith(letterSpacing: -0.75, fontWeight: FontWeight.w800),
      headlineLarge: t.headlineLarge?.copyWith(letterSpacing: -0.5, fontWeight: FontWeight.w800),
      headlineMedium: t.headlineMedium?.copyWith(letterSpacing: -0.5, fontWeight: FontWeight.w700),
      headlineSmall: t.headlineSmall?.copyWith(letterSpacing: -0.25, fontWeight: FontWeight.w700),
      titleLarge: t.titleLarge?.copyWith(letterSpacing: -0.15, fontWeight: FontWeight.w700),
      titleMedium: t.titleMedium?.copyWith(fontWeight: FontWeight.w600),
      bodyLarge: t.bodyLarge?.copyWith(height: 1.45),
      bodyMedium: t.bodyMedium?.copyWith(height: 1.45),
    );

final appTheme = ThemeData(
  useMaterial3: true,
  colorScheme: ColorScheme.fromSeed(seedColor: desktopAccent).copyWith(
    primary: desktopAccent,
    onPrimary: Colors.white,
  ),
  scaffoldBackgroundColor: const Color(0xFFF6F7F9),
  textTheme: _tight(_baseText),
  appBarTheme: AppBarTheme(
    centerTitle: false,
    backgroundColor: desktopAccent,
    foregroundColor: Colors.white,
    titleTextStyle: GoogleFonts.plusJakartaSans(
        fontSize: 19, fontWeight: FontWeight.w700, color: Colors.white, letterSpacing: -0.2),
  ),
  inputDecorationTheme: InputDecorationTheme(
    border: OutlineInputBorder(borderRadius: BorderRadius.circular(14)),
    focusedBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: const BorderSide(color: desktopAccent, width: 2),
    ),
  ),
  filledButtonTheme: FilledButtonThemeData(
    style: FilledButton.styleFrom(
      backgroundColor: desktopAccent,
      foregroundColor: Colors.white,
      textStyle: GoogleFonts.plusJakartaSans(fontWeight: FontWeight.w700, fontSize: 15),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
    ),
  ),
);
