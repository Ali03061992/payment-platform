/** Enveloppe paginée standard du backend : {items, totalElements, totalPages, number}. */
export interface PageResponse<T> {
  items: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}

/** Découpe côté client une liste déjà chargée (endpoints sans pagination serveur). */
export function paginateItems<T>(items: T[], page: number, size: number): T[] {
  if (!items || items.length === 0) return [];
  const start = page * size;
  if (start >= items.length) return [];
  return items.slice(start, start + size);
}

/** Bornes d'affichage "X-Y sur Z" (1-based) pour une page 0-based. */
export function pageRange(page: number, size: number, total: number): { from: number; to: number } {
  if (total === 0) return { from: 0, to: 0 };
  const from = page * size + 1;
  const to = Math.min((page + 1) * size, total);
  if (from > total) return { from: 0, to: 0 };
  return { from, to };
}

/** Fenêtre de numéros de page (max 5) centrée sur la page courante. */
export function pageWindow(current: number, totalPages: number, maxVisible = 5): number[] {
  const pages: number[] = [];
  if (totalPages <= 0) return pages;
  let start = Math.max(0, current - Math.floor(maxVisible / 2));
  const end = Math.min(totalPages, start + maxVisible);
  if (end - start < maxVisible) start = Math.max(0, end - maxVisible);
  for (let i = start; i < end; i++) pages.push(i);
  return pages;
}

/** Sens de tri d'une colonne (ascendant / descendant). */
export type SortDirection = 'asc' | 'desc';

/** État de tri homogène des tableaux (tri côté client sur la page chargée). */
export interface SortState {
  field: string | null;
  direction: SortDirection;
}

/** Bascule le tri sur une colonne : 1er clic asc, 2e desc, 3e on repart sur asc. */
export function toggleSortState(current: SortState, field: string): SortState {
  if (current.field !== field) return { field, direction: 'asc' };
  return { field, direction: current.direction === 'asc' ? 'desc' : 'asc' };
}

/** Libellé aria-sort pour un en-tête de colonne triable. */
export function ariaSortFor(field: string, current: SortState): 'ascending' | 'descending' | 'none' {
  if (current.field !== field) return 'none';
  return current.direction === 'asc' ? 'ascending' : 'descending';
}

/** Indicateur visuel de tri (⇅ par défaut, ↑ asc, ↓ desc). */
export function sortIndicatorFor(field: string, current: SortState): string {
  if (current.field !== field) return '⇅';
  return current.direction === 'asc' ? '↑' : '↓';
}

function comparableValue(value: unknown): string | number {
  if (value === null || value === undefined) return '';
  if (value instanceof Date) return value.getTime();
  if (typeof value === 'number') return Number.isNaN(value) ? 0 : value;
  if (typeof value === 'boolean') return value ? 1 : 0;
  const num = typeof value === 'string' && value.trim() !== '' ? Number(value) : NaN;
  if (value instanceof String) return String(value);
  if (typeof value === 'string' && !Number.isNaN(num) && /^-?\d+(\.\d+)?$/.test(value.trim())) return num;
  return String(value).toLocaleLowerCase('fr');
}

/** Trie côté client une liste sur un champ (nombres, dates ISO, chaînes FR). */
export function sortItems<T>(items: T[], field: string | null, direction: SortDirection = 'asc'): T[] {
  if (!field || !items || items.length <= 1) return items ? [...items] : [];
  const dir = direction === 'asc' ? 1 : -1;
  return [...items].sort((a, b) => {
    const av = comparableValue((a as Record<string, unknown>)[field]);
    const bv = comparableValue((b as Record<string, unknown>)[field]);
    if (typeof av === 'number' && typeof bv === 'number') return (av - bv) * dir;
    const as = String(av);
    const bs = String(bv);
    if (as === bs) return 0;
    return (as < bs ? -1 : 1) * dir;
  });
}
