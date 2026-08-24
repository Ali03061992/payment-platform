import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProductCategory, ProductFamily, ProductSubfamily } from '../models/catalog.model';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private apiUrl = '/api/supplier/catalog';

  constructor(private http: HttpClient) {}

  listCategories(supplierId: number): Observable<ProductCategory[]> {
    return this.http.get<ProductCategory[]>(`${this.apiUrl}/${supplierId}/categories`);
  }

  createCategory(data: { supplierId: number; name: string; code: string }): Observable<ProductCategory> {
    return this.http.post<ProductCategory>(`${this.apiUrl}/${data.supplierId}/categories`, { name: data.name, code: data.code });
  }

  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/categories/${id}`);
  }

  listFamilies(supplierId: number, categoryId?: number): Observable<ProductFamily[]> {
    let url = `${this.apiUrl}/${supplierId}/families`;
    if (categoryId != null) {
      url += `?categoryId=${categoryId}`;
    }
    return this.http.get<ProductFamily[]>(url);
  }

  createFamily(data: { supplierId: number; categoryId: number; name: string; code: string }): Observable<ProductFamily> {
    return this.http.post<ProductFamily>(`${this.apiUrl}/${data.supplierId}/families`, { categoryId: data.categoryId, name: data.name, code: data.code });
  }

  deleteFamily(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/families/${id}`);
  }

  listSubfamilies(supplierId: number, familyId?: number): Observable<ProductSubfamily[]> {
    let url = `${this.apiUrl}/${supplierId}/subfamilies`;
    if (familyId != null) {
      url += `?familyId=${familyId}`;
    }
    return this.http.get<ProductSubfamily[]>(url);
  }

  createSubfamily(data: { supplierId: number; familyId: number; name: string; code: string }): Observable<ProductSubfamily> {
    return this.http.post<ProductSubfamily>(`${this.apiUrl}/${data.supplierId}/subfamilies`, { familyId: data.familyId, name: data.name, code: data.code });
  }

  deleteSubfamily(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/subfamilies/${id}`);
  }
}
