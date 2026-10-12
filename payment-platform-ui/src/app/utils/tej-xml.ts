/** Construction côté frontend du XML d'échange TEJ (saisie libre — sans persistance).
 *  Miroir documenté de WithholdingXmlService (backend) : mentions art. 55 IRPP/IS
 *  + arrêté du 10 mai 2024. Le XSD officiel du ministère reste la référence.
 */

export interface TejManualData {
  number: string;
  debtorName: string;
  debtorMatricule: string;
  debtorAddress: string;
  beneficiaryName: string;
  beneficiaryMatricule: string;
  beneficiaryAddress: string;
  nature: string;
  grossAmount: number;
  rate: number;
  withholdingAmount: number;
  netAmount: number;
  currency: string;
  paymentReference: string;
  paymentDate: string; // AAAA-MM-JJ
  billingPeriod: string;
  dueDate: string; // AAAA-MM-JJ
  depositorTejId?: string | null;
}

/** Taux RAS par nature (art. 52) — miroir de WithholdingNature. */
export const WITHHOLDING_RATES: Record<string, { rate: number; thresholdTtc: number | null }> = {
  MARCHANDISES: { rate: 1.5, thresholdTtc: 1000 },
  HONORAIRES_REEL: { rate: 3, thresholdTtc: null },
  HONORAIRES_FORFAIT: { rate: 10, thresholdTtc: null },
  NON_RESIDENT: { rate: 15, thresholdTtc: null },
  INTERETS: { rate: 20, thresholdTtc: null },
  DIVIDENDES: { rate: 10, thresholdTtc: null },
  PLUS_VALUE_IMMO: { rate: 2.5, thresholdTtc: null },
  SANS_MF: { rate: 15, thresholdTtc: null }
};

export const WITHHOLDING_NATURES = Object.keys(WITHHOLDING_RATES);

export function round3(n: number): number {
  return Math.round(n * 1000) / 1000;
}

/** Résout nature effective (sans MF => SANS_MF 15%) + montants. */
export function computeManualWithholding(grossTtc: number, declaredNature: string, beneficiaryMf: string): { nature: string; rate: number; withholding: number; net: number } {
  const nature = !beneficiaryMf || !beneficiaryMf.trim() ? 'SANS_MF' : (declaredNature || 'MARCHANDISES');
  const def = WITHHOLDING_RATES[nature] || WITHHOLDING_RATES['MARCHANDISES'];
  const rate = def.thresholdTtc != null && grossTtc < def.thresholdTtc ? 0 : def.rate;
  const withholding = round3(grossTtc * rate / 100);
  return { nature, rate, withholding, net: round3(grossTtc - withholding) };
}

/** Échéance légale : fin du mois suivant le paiement. */
export function tejDueDate(paymentDateIso: string): string {
  const d = new Date(paymentDateIso + 'T00:00:00');
  const y = d.getFullYear();
  const m = d.getMonth() + 1; // 0-based -> mois suivant
  const ny = m === 12 ? y + 1 : y;
  const nm = m === 12 ? 1 : m + 1;
  const last = new Date(ny, nm, 0).getDate();
  return `${ny}-${String(nm).padStart(2, '0')}-${String(last).padStart(2, '0')}`;
}

function fmt(n: number): string {
  return (Math.round(n * 1000) / 1000).toFixed(3);
}

function esc(v: string | null | undefined): string {
  return (v || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&apos;');
}

export function buildTejXml(c: TejManualData): string {
  const lines = [
    '<?xml version="1.0" encoding="UTF-8"?>'
  ];
  if (c.depositorTejId && c.depositorTejId.trim()) {
    lines.push(`<!-- deposant TEJ: ${esc(c.depositorTejId.trim())} -->`);
  }
  lines.push('<certificatsRetenue version="1.0" xmlns="urn:tn:finances:tej:retenue:v1">');
  lines.push('  <certificat>');
  lines.push(`    <numero>${esc(c.number)}</numero>`);
  lines.push('    <debiteur>');
  lines.push(`      <matriculeFiscal>${esc(c.debtorMatricule)}</matriculeFiscal>`);
  lines.push(`      <raisonSociale>${esc(c.debtorName)}</raisonSociale>`);
  lines.push(`      <adresse>${esc(c.debtorAddress)}</adresse>`);
  lines.push('    </debiteur>');
  lines.push('    <beneficiaire>');
  lines.push(`      <matriculeFiscal>${esc(c.beneficiaryMatricule)}</matriculeFiscal>`);
  lines.push(`      <raisonSociale>${esc(c.beneficiaryName)}</raisonSociale>`);
  lines.push(`      <adresse>${esc(c.beneficiaryAddress)}</adresse>`);
  lines.push('    </beneficiaire>');
  lines.push('    <facture>');
  lines.push(`      <reference>${esc(c.paymentReference)}</reference>`);
  lines.push(`      <date>${esc(c.paymentDate)}</date>`);
  lines.push(`      <nature>${esc(c.nature)}</nature>`);
  lines.push(`      <exerciceFacturation>${esc(c.billingPeriod)}</exerciceFacturation>`);
  lines.push('    </facture>');
  lines.push('    <montants>');
  lines.push(`      <brut>${fmt(c.grossAmount)}</brut>`);
  lines.push(`      <taux>${fmt(c.rate)}</taux>`);
  lines.push(`      <retenue>${fmt(c.withholdingAmount)}</retenue>`);
  lines.push(`      <net>${fmt(c.netAmount)}</net>`);
  lines.push(`      <devise>${esc(c.currency)}</devise>`);
  lines.push('    </montants>');
  lines.push(`    <echeance>${esc(c.dueDate)}</echeance>`);
  lines.push('  </certificat>');
  lines.push('</certificatsRetenue>');
  return lines.join('\n') + '\n';
}

/** Heuristiques d'extraction depuis le texte d'une facture PDF (valeurs à vérifier). */
export interface PdfHints {
  matricules: string[];
  amounts: number[];
  raw: string;
}

const MF_RE = /\b\d{7,8}[A-Z]?\/[ABDPN]\/[MPN]\/\d{3}\b/g;
const AMOUNT_RE = /(\d[\d\s]*[.,]\d{3})\s*(DT|TND|د\.?ت\.?)?/g;

export function extractPdfHints(text: string): PdfHints {
  const matricules = [...new Set((text.match(MF_RE) || []).map(s => s.replace(/\s+/g, '')))];
  const amounts: number[] = [];
  let m: RegExpExecArray | null;
  AMOUNT_RE.lastIndex = 0;
  while ((m = AMOUNT_RE.exec(text)) !== null && amounts.length < 20) {
    const v = parseFloat(m[1].replace(/\s+/g, '').replace(',', '.'));
    if (isFinite(v) && v > 0) amounts.push(Math.round(v * 1000) / 1000);
  }
  return { matricules, amounts: [...new Set(amounts)].sort((a, b) => b - a), raw: text.slice(0, 4000) };
}
