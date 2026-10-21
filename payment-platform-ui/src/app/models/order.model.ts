export interface Order {
  id: string;
  reference: string;
  supplierId: string;
  shopId: string;
  supplierName: string | null;
  shopName: string | null;
  createdBy: string;
  createdByName: string | null;
  createdByRole: string;
  source: string;
  status: string;
  subtotal: number;
  taxRate: number;
  taxAmount: number;
  fodecTotal: number;
  stampAmount: number;
  netPayable: number;
  vatBreakdown: VatBreakdownLine[];
  fiscalMention: string | null;
  exemptionReason: string | null;
  buyerRegime: string | null;
  externalReference: string | null;
  total: number;
  currency: string;
  deliveryAgentId: string | null;
  deliveryAgentName: string | null;
  receivedBy: string | null;
  receivedByName: string | null;
  receivedAt: string | null;
  deliveredAt: string | null;
  plannedDeliveryDate: string | null;
  confirmedDeliveryDate: string | null;
  asapPayment: boolean;
  paymentTerms: string | null;
  dueDate: string | null;
  deliveryRejectionReason: string | null;
  estimatedArrival: string | null;
  lastLatitude: number | null;
  lastLongitude: number | null;
  lastLocationUpdate: string | null;
  notes: string | null;
  confirmedByName: string | null;
  preparedByName: string | null;
  readyByName: string | null;
  assignedDeliveryByName: string | null;
  acceptedDeliveryByName: string | null;
  confirmedDeliveryByName: string | null;
  deliveredByName: string | null;
  version: number;
  supplierTeifEnabled?: boolean;
  supplierTejEnabled?: boolean;
  createdAt: string;
  updatedAt: string;
  items: OrderItem[];
  events: OrderEvent[];
}

export interface VatBreakdownLine {
  rate: number;
  base: number;
  amount: number;
}

export interface OrderItem {
  id: string;
  orderId: string;
  productId: string;
  productRef: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  discount: number;
  lineTotal: number;
  vatRate: number;
  fodecApplicable: boolean;
  priceTtc: boolean;
  htUnitPrice: number | null;
  fodecAmount: number;
  vatBase: number;
  vatAmount: number;
  createdAt: string;
}

/** Certificat de retenue à la source rattaché à la facture de commande (art. 55 IRPP/IS, TEJ). */
export interface WithholdingCertificate {
  id: string;
  orderId: string;
  number: string;
  debtorId: string;
  debtorName: string;
  debtorMatricule: string | null;
  debtorAddress: string | null;
  beneficiaryId: string;
  beneficiaryName: string;
  beneficiaryMatricule: string | null;
  beneficiaryAddress: string | null;
  nature: string;
  grossAmount: number;
  rate: number;
  withholdingAmount: number;
  netAmount: number;
  currency: string;
  orderReference: string;
  orderDate: string;
  billingPeriod: string | null;
  dueDate: string;
  status: 'READY' | 'DEPOSITED' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED';
  tejReference: string | null;
  createdAt: string;
}

/** Synthèse mensuelle des retenues d'un débiteur (réconciliation déclaration TEJ). */
export interface WithholdingMonthlySummary {
  month: string;
  certificateCount: number;
  grossTotal: number;
  withholdingTotal: number;
  byNature: Record<string, number>;
}

/** Demande de devis boutique (entité dédiée, séparée des commandes). */
export interface QuoteRequest {
  id: string;
  reference: string;
  supplierId: string;
  supplierName: string | null;
  shopId: string;
  shopName: string | null;
  notes: string | null;
  status: 'PENDING' | 'QUOTED' | 'ACCEPTED' | 'CANCELLED';
  linesJson: string;
  createdBy: string | null;
  createdAt: string;
  quotes: SupplierQuote[];
}

/** Devis fournisseur versionné rattaché à une demande. */
export interface SupplierQuote {
  id: string;
  requestId: string;
  requestReference: string;
  supplierId: string;
  version: number;
  subtotal: number;
  taxAmount: number;
  total: number;
  stampAmount: number;
  currency: string;
  linesJson: string;
  createdBy: string | null;
  createdAt: string;
}

export interface QuoteLine {
  productId?: string;
  ref: string;
  name: string;
  qty: number;
  quantity?: number;
  unitPrice: number;
  discountPct: number;
  discount: number;
  lineTotal: number;
  vatRate: number;
}

export interface OrderEvent {
  id: string;
  orderId: string;
  action: string;
  userId: string | null;
  timestamp: string;
  details: string | null;
}

export interface CreateOrderRequest {
  supplierId: string;
  shopId: string;
  asapPayment: boolean;
  paymentTerms: string;
  currency: string;
  taxRate?: number;
  notes: string;
  items: { productId: string; quantity: number; discount: number }[];
}

export interface UpdateOrderRequest {
  notes: string | null;
  asapPayment: boolean;
  items: { productId: string; quantity: number; discount: number }[];
}

export interface OrderComment {
  id: string;
  orderId: string;
  authorId: string;
  authorName: string;
  content: string;
  createdAt: string;
}

/** Page de commandes telle que retournée par GET /api/orders (page/size, status). */
export interface OrderPage {
  items: Order[];
  totalElements: number;
  totalPages: number;
  number: number;
}

/** Ligne du journal des factures (GET /api/orders/invoices/journal). */
export interface InvoiceJournalEntry {
  id: string;
  invoiceNumber: string;
  orderId: string;
  orderReference: string;
  supplierId: string;
  supplierName: string | null;
  shopId: string;
  shopName: string | null;
  totalTtc: number;
  vatAmount: number | null;
  currency: string;
  issuedAt: string;
}

/** Page du journal des factures. */
export interface InvoiceJournalPage {
  items: InvoiceJournalEntry[];
  totalElements: number;
  totalPages: number;
  number: number;
}
