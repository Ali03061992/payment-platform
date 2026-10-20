/**
 * Configuration centralisée de la marque NEXTERA.
 *
 * Source unique pour le nom produit, les couleurs et les assets de marque.
 * Les fichiers statiques (index.html, manifest.json, service worker) ne
 * peuvent pas importer ce module TypeScript : ils dupliquent ces valeurs,
 * à garder synchronisées en cas de changement.
 */
export const BRAND = {
  name: 'NEXTERA',
  shortName: 'NEXTERA',
  logoPath: 'assets/icon.png',
  primaryColor: '#12243A',
  accentColor: '#18A999',
  backgroundColor: '#FFFFFF'
} as const;