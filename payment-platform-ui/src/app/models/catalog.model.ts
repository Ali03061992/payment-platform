export interface ProductCategory {
  id: string;
  supplierId: string;
  name: string;
  code: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProductFamily {
  id: string;
  supplierId: string;
  name: string;
  code: string;
  status: string;
  categories: ProductCategory[];
  createdAt: string;
  updatedAt: string;
}
