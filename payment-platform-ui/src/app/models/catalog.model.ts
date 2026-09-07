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
  name: string;
  code: string;
  status: string;
  categories: ProductCategory[];
  createdAt: string;
  updatedAt: string;
}
