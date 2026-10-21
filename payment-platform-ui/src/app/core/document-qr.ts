export type DocumentQrType =
  | 'ORDER'
  | 'INVOICE'
  | 'QUOTE'
  | 'DELIVERY'
  | 'PAYMENT'
  | 'DISPUTE'
  | 'WITHHOLDING_CERTIFICATE';

export interface DocumentQrPayload {
  v?: number;
  type: DocumentQrType;
  reference: string;
}

export function documentQrData(type: DocumentQrType, reference: string): string {
  return JSON.stringify({ v: 1, type, reference: reference || '' });
}

export function parseDocumentQr(data: string): DocumentQrPayload | null {
  const raw = (data || '').trim();
  if (!raw.startsWith('{')) return null;
  try {
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed.type !== 'string' || typeof parsed.reference !== 'string') {
      return null;
    }
    return { v: parsed.v, type: parsed.type as DocumentQrType, reference: parsed.reference };
  } catch {
    return null;
  }
}

export function documentQrRoute(payload: DocumentQrPayload): string | null {
  const ref = encodeURIComponent(payload.reference);
  if (!payload.reference) return null;
  switch (payload.type) {
    case 'ORDER':
    case 'DELIVERY':
      return `/dashboard/orders/${ref}`;
    case 'INVOICE':
      return `/dashboard/invoices/${ref}`;
    case 'QUOTE':
      return `/dashboard/quotes/${ref}`;
    case 'PAYMENT':
      return `/dashboard/payments/${ref}`;
    case 'DISPUTE':
      return `/dashboard/disputes/${ref}`;
    case 'WITHHOLDING_CERTIFICATE':
      return `/dashboard/certificates/${ref}`;
    default:
      return null;
  }
}
