import { documentQrData, documentQrRoute, parseDocumentQr } from './document-qr';

describe('document QR payload', () => {
  it('serializes a versioned JSON payload', () => {
    expect(documentQrData('ORDER', 'CMD-42')).toBe('{"v":1,"type":"ORDER","reference":"CMD-42"}');
  });

  it('parses a JSON payload', () => {
    expect(parseDocumentQr('{"v":1,"type":"INVOICE","reference":"INV-9"}'))
      .toEqual({ v: 1, type: 'INVOICE', reference: 'INV-9' });
  });

  it('rejects invalid or incomplete payloads', () => {
    expect(parseDocumentQr('FACTURE:F-1|DOC:CMD-1')).toBeNull();
    expect(parseDocumentQr('{"type":"ORDER"}')).toBeNull();
    expect(parseDocumentQr('{oops}')).toBeNull();
  });

  it('maps every type to its detail route', () => {
    expect(documentQrRoute({ type: 'ORDER', reference: 'CMD-1' })).toBe('/dashboard/orders/CMD-1');
    expect(documentQrRoute({ type: 'DELIVERY', reference: 'CMD-1' })).toBe('/dashboard/orders/CMD-1');
    expect(documentQrRoute({ type: 'INVOICE', reference: 'INV-1' })).toBe('/dashboard/invoices/INV-1');
    expect(documentQrRoute({ type: 'QUOTE', reference: 'DEV-1' })).toBe('/dashboard/quotes/DEV-1');
    expect(documentQrRoute({ type: 'PAYMENT', reference: 'PAY-1' })).toBe('/dashboard/payments/PAY-1');
    expect(documentQrRoute({ type: 'DISPUTE', reference: 'D-1' })).toBe('/dashboard/disputes/D-1');
    expect(documentQrRoute({ type: 'WITHHOLDING_CERTIFICATE', reference: 'CRT-1' }))
      .toBe('/dashboard/certificates/CRT-1');
  });

  it('encodes references and rejects an empty reference', () => {
    expect(documentQrRoute({ type: 'INVOICE', reference: 'INV/2026 01' }))
      .toBe('/dashboard/invoices/INV%2F2026%2001');
    expect(documentQrRoute({ type: 'ORDER', reference: '' })).toBeNull();
  });
});
