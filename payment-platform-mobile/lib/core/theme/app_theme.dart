import 'package:flutter/material.dart';

/// Palette desktop (styles.css) : accent #0f3460, texte #1a1a2e/#333/#5f7186.
const desktopAccent = Color(0xFF0F3460);
const desktopInk = Color(0xFF1A1A2E);

final appTheme = ThemeData(
  useMaterial3: true,
  colorScheme: ColorScheme.fromSeed(seedColor: desktopAccent).copyWith(
    primary: desktopAccent,
    onPrimary: Colors.white,
  ),
  scaffoldBackgroundColor: const Color(0xFFF5F6FA),
  appBarTheme: const AppBarTheme(
    centerTitle: false,
    backgroundColor: desktopAccent,
    foregroundColor: Colors.white,
  ),
  inputDecorationTheme: InputDecorationTheme(
    border: OutlineInputBorder(borderRadius: BorderRadius.circular(10)),
    focusedBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(10),
      borderSide: const BorderSide(color: desktopAccent, width: 2),
    ),
  ),
  filledButtonTheme: FilledButtonThemeData(
    style: FilledButton.styleFrom(
      backgroundColor: desktopAccent,
      foregroundColor: Colors.white,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
    ),
  ),
);
