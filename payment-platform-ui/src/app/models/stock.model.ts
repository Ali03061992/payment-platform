export interface Product {
  id: string;
  supplierId: string;
  name: string;
  sku: string;
  description: string;
  imageUrl: string | null;
  unitPrice: number;
  currency: string;
  quantity: number;
  minQuantity: number;
  reservedQty: number;
  categoryId: string | null;
  familyId: string | null;
  unit: string;
  status: 'ACTIVE' | 'INACTIVE' | 'OUT_OF_STOCK';
  createdAt: string;
  updatedAt: string;
  vatRate?: number;
  fodecApplicable?: boolean;
  vatExemptionText?: string | null;
  priceTtc?: boolean;
}

/** Taux TVA tunisiens légaux par produit (0 = exonéré avec motif). */
export const TUNISIAN_VAT_RATES = [19, 13, 7, 0] as const;

export interface ProductCreateRequest {
  name: string;
  sku: string;
  description: string;
  unitPrice: number;
  currency: string;
  quantity: number;
  minQuantity: number;
  categoryId?: string | null;
  familyId?: string | null;
  unit?: string;
  vatRate?: number;
  fodecApplicable?: boolean;
  vatExemptionText?: string | null;
  priceTtc?: boolean;
}

export interface ProductUpdateRequest {
  name?: string;
  description?: string;
  unitPrice?: number;
  quantity?: number;
  minQuantity?: number;
  status?: string;
  categoryId?: string | null;
  familyId?: string | null;
  unit?: string;
  vatRate?: number;
  fodecApplicable?: boolean;
  vatExemptionText?: string | null;
  priceTtc?: boolean;
}

export interface StockMovement {
  id: string;
  productId: string;
  productName: string;
  type: 'IN' | 'OUT' | 'ADJUSTMENT' | 'RETURN';
  quantity: number;
  reference: string;
  notes: string;
  createdAt: string;
  createdBy: string;
}

export interface StockMovementRequest {
  type: 'IN' | 'OUT' | 'ADJUSTMENT' | 'RETURN';
  quantity: number;
  reference: string;
  notes: string;
}
