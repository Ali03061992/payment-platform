export interface Product {
  id: string;
  supplierId: string;
  name: string;
  sku: string;
  description: string;
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
}

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
}

export interface StockMovement {
  id: string;
  productId: string;
  productName: string;
  type: 'IN' | 'OUT' | 'ADJUSTMENT';
  quantity: number;
  reference: string;
  notes: string;
  createdAt: string;
  createdBy: string;
}

export interface StockMovementRequest {
  type: 'IN' | 'OUT' | 'ADJUSTMENT';
  quantity: number;
  reference: string;
  notes: string;
}
