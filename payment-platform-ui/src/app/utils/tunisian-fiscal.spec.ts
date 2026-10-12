import { previewFiscal } from './tunisian-fiscal';

describe('previewFiscal (fiscalité tunisienne)', () => {
  it('should apply standard 19% on net HT', () => {
    const r = previewFiscal(
      [{ unitPrice: 1000, quantity: 1, discountPct: 0, vatRate: 19, fodecApplicable: false }], 0);
    expect(r.netHt).toBeCloseTo(1000, 4);
    expect(r.fodec).toBeCloseTo(0, 4);
    expect(r.vatTotal).toBeCloseTo(190, 4);
    expect(r.ttc).toBeCloseTo(1190, 4);
    expect(r.stamp).toBe(1);
    expect(r.netPayable).toBeCloseTo(1191, 4);
  });

  it('should include FODEC 1% in the VAT base', () => {
    const r = previewFiscal(
      [{ unitPrice: 1000, quantity: 1, discountPct: 0, vatRate: 19, fodecApplicable: true }], 0);
    expect(r.fodec).toBeCloseTo(10, 4);
    expect(r.vatTotal).toBeCloseTo(191.9, 4);
    expect(r.ttc).toBeCloseTo(1201.9, 4);
  });

  it('should deduct global discount before VAT (10000 - 5%)', () => {
    const r = previewFiscal(
      [{ unitPrice: 10000, quantity: 1, discountPct: 0, vatRate: 19, fodecApplicable: false }], 5);
    expect(r.discountAmount).toBeCloseTo(500, 4);
    expect(r.netHt).toBeCloseTo(9500, 4);
    expect(r.vatTotal).toBeCloseTo(1805, 4);
    expect(r.ttc).toBeCloseTo(11305, 4);
    expect(r.netPayable).toBeCloseTo(11306, 4);
  });

  it('should ventilate mixed 19% + 7% lines by rate', () => {
    const r = previewFiscal([
      { unitPrice: 1000, quantity: 1, discountPct: 0, vatRate: 19, fodecApplicable: false },
      { unitPrice: 500, quantity: 1, discountPct: 0, vatRate: 7, fodecApplicable: false }
    ], 0);
    expect(r.vatByRate.length).toBe(2);
    expect(r.vatByRate[0].rate).toBe(19);
    expect(r.vatByRate[0].amount).toBeCloseTo(190, 4);
    expect(r.vatByRate[1].rate).toBe(7);
    expect(r.vatByRate[1].amount).toBeCloseTo(35, 4);
  });

  it('should default missing rate to 19', () => {
    const r = previewFiscal(
      [{ unitPrice: 100, quantity: 1, discountPct: 0, vatRate: null, fodecApplicable: false }], 0);
    expect(r.vatTotal).toBeCloseTo(19, 4);
  });

  it('should deduct VAT for TTC prices instead of adding it', () => {
    const r = previewFiscal(
      [{ unitPrice: 1190, quantity: 1, discountPct: 0, vatRate: 19, fodecApplicable: false, priceTtc: true }], 0);
    expect(r.netHt).toBeCloseTo(1000, 4);
    expect(r.vatTotal).toBeCloseTo(190, 4);
    expect(r.ttc).toBeCloseTo(1190, 4);
  });
});
