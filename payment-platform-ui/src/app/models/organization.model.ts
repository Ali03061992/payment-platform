export interface Organization {
  id: number;
  name: string;
  type: 'SUPPLIER' | 'SHOP';
  status: 'ACTIVE' | 'DISABLED';
  version: number;
  createdAt: string;
  updatedAt: string;
  relations: SupplierShopRelation[];
}

export interface SupplierShopRelation {
  id: number;
  supplierId: number;
  shopId: number;
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
}

export interface OrganizationStats {
  suppliers: number;
  shops: number;
}

export interface CreateOrganizationRequest {
  name: string;
}

export interface CreateRelationRequest {
  supplierId: number;
  shopId: number;
}
