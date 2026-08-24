export interface ProductCategory {
  id: number;
  supplierId: number;
  name: string;
  code: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProductFamily {
  id: number;
  supplierId: number;
  categoryId: number;
  name: string;
  code: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProductSubfamily {
  id: number;
  supplierId: number;
  familyId: number;
  name: string;
  code: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}
