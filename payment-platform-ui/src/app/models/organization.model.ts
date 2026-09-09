export interface Organization {
  id: string;
  name: string;
  type: 'SUPPLIER' | 'SHOP';
  status: 'ACTIVE' | 'DISABLED';
  version: number;
  createdAt: string;
  updatedAt: string;
  relations: SupplierShopRelation[];
}

export interface SupplierShopRelation {
  id: string;
  supplierId: string;
  shopId: string;
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
  supplierId: string;
  shopId: string;
}
