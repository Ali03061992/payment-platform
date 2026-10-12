import { buildTejXml, computeManualWithholding, extractPdfHints, tejDueDate, TejManualData } from './tej-xml';

describe('tej-xml (préparation dépôt TEJ)', () => {
  it('should compute 1.5% above threshold and 0 below', () => {
    expect(computeManualWithholding(10000, 'MARCHANDISES', 'MF')).toEqual({ nature: 'MARCHANDISES', rate: 1.5, withholding: 150, net: 9850 });
    expect(computeManualWithholding(500, 'MARCHANDISES', 'MF').withholding).toBe(0);
  });

  it('should force SANS_MF 15% without beneficiary MF', () => {
    const r = computeManualWithholding(2000, 'MARCHANDISES', '');
    expect(r.nature).toBe('SANS_MF');
    expect(r.withholding).toBe(300);
  });

  it('should compute end of next month', () => {
    expect(tejDueDate('2026-07-15')).toBe('2026-08-31');
    expect(tejDueDate('2026-12-01')).toBe('2027-01-31');
  });

  it('should build XML with legal mentions and 3 decimals', () => {
    const data: TejManualData = {
      number: 'CRT-MAN-001', debtorName: 'D', debtorMatricule: 'DMF', debtorAddress: 'DA',
      beneficiaryName: 'B', beneficiaryMatricule: 'BMF', beneficiaryAddress: 'BA',
      nature: 'MARCHANDISES', grossAmount: 10000, rate: 1.5, withholdingAmount: 150, netAmount: 9850,
      currency: 'TND', paymentReference: 'PAY-1', paymentDate: '2026-07-15', billingPeriod: '2026-07',
      dueDate: '2026-08-31', depositorTejId: 'TEJ-9'
    };
    const xml = buildTejXml(data);
    expect(xml).toContain('<!-- deposant TEJ: TEJ-9 -->');
    expect(xml).toContain('<brut>10000.000</brut>');
    expect(xml).toContain('<retenue>150.000</retenue>');
    expect(xml).toContain('<echeance>2026-08-31</echeance>');
  });

  it('should extract MF and amounts hints from invoice text', () => {
    const hints = extractPdfHints('MF: 1234567A/A/M/000 Total TTC 11 901,000 DT TVA 1 900,000');
    expect(hints.matricules).toContain('1234567A/A/M/000');
    expect(hints.amounts).toContain(11901);
    expect(hints.amounts).toContain(1900);
  });
});
