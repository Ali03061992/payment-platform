export interface Product {
  id: number;
  supplierId: number;
  name: string;
  sku: string;
  description: string;
  unitPrice: number;
  currency: string;
  quantity: number;
  minQuantity: number;
  reservedQty: number;
  categoryId: number | null;
  familyId: number | null;
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
  categoryId?: number | null;
  familyId?: number | null;
  unit?: string;
}

export interface ProductUpdateRequest {
  name?: string;
  description?: string;
  unitPrice?: number;
  quantity?: number;
  minQuantity?: number;
  status?: string;
  categoryId?: number | null;
  familyId?: number | null;
  unit?: string;
}

export interface StockMovement {
  id: number;
  productId: number;
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
