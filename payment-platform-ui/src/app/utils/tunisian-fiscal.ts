/** Aperçu fiscal tunisien côté frontend (estimation — le backend recalcule). Ordre légal : remise ligne → FODEC → base TVA → TVA par taux → timbre. Aucune remise globale. */

export interface FiscalLineInput {
  unitPrice: number;
  quantity: number;
  /** Remise ligne en % (convention d'affichage des écrans de commande). */
  discountPct: number;
  vatRate: number | null | undefined;
  fodecApplicable: boolean | null | undefined;
  /** Prix TTC : TVA incluse, déduite au lieu d'être ajoutée. */
  priceTtc?: boolean | null;
}

export interface VatPreviewLine {
  rate: number;
  base: number;
  amount: number;
}

export interface FiscalPreview {
  subtotal: number;
  discountAmount: number;
  netHt: number;
  fodec: number;
  vatByRate: VatPreviewLine[];
  vatTotal: number;
  ttc: number;
  stamp: number;
  netPayable: number;
}

export const TUNISIAN_VAT_RATES = [19, 13, 7, 0];

function round4(n: number): number {
  return Math.round(n * 10000) / 10000;
}

export function previewFiscal(
  lines: FiscalLineInput[],
  stamp = 1
): FiscalPreview {
  let subtotal = 0;
  let discountAmount = 0;
  let netHt = 0;
  let fodec = 0;
  const byRate = new Map<number, { base: number; amount: number }>();

  for (const l of lines) {
    const rate = l.vatRate ?? 19;
    // Montant remise = (PU × Qté) × remise% / 100 ; net = brut − remise.
    const lineGross = l.unitPrice * l.quantity;
    const lineDiscount = lineGross * ((l.discountPct || 0) / 100);
    const netEntered = lineGross - lineDiscount;
    const net = l.priceTtc && rate > 0 ? netEntered / (1 + rate / 100) : netEntered;
    const lineFodec = l.fodecApplicable ? net * 0.01 : 0;
    const base = net + lineFodec;
    const vat = base * (rate / 100);
    subtotal += net;
    discountAmount += lineDiscount;
    netHt += net;
    fodec += lineFodec;
    const prev = byRate.get(rate) || { base: 0, amount: 0 };
    byRate.set(rate, { base: prev.base + base, amount: prev.amount + vat });
  }

  const vatByRate: VatPreviewLine[] = [...byRate.entries()]
    .sort((a, b) => b[0] - a[0])
    .map(([rate, v]) => ({ rate, base: round4(v.base), amount: round4(v.amount) }));
  const vatTotal = round4(vatByRate.reduce((s, v) => s + v.amount, 0));
  const ttc = round4(netHt + fodec + vatTotal);

  return {
    subtotal: round4(subtotal),
    discountAmount: round4(discountAmount),
    netHt: round4(netHt),
    fodec: round4(fodec),
    vatByRate,
    vatTotal,
    ttc,
    stamp,
    netPayable: round4(ttc + stamp)
  };
}
