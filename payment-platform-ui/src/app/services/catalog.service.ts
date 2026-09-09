import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProductCategory, ProductFamily } from '../models/catalog.model';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private apiUrl = '/api/supplier/catalog';

  constructor(private http: HttpClient) {}

  listCategories(supplierId: string): Observable<ProductCategory[]> {
    return this.http.get<ProductCategory[]>(`${this.apiUrl}/categories`, {
      params: { supplierId: supplierId.toString() }
    });
  }

  createCategory(data: { supplierId: string; name: string; code: string }): Observable<ProductCategory> {
    return this.http.post<ProductCategory>(`${this.apiUrl}/categories`, data);
  }

  deleteCategory(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/categories/${id}`);
  }

  listFamilies(supplierId: string, categoryId?: string): Observable<ProductFamily[]> {
    let params = new HttpParams().set('supplierId', supplierId.toString());
    if (categoryId != null) {
      params = params.set('categoryId', categoryId.toString());
    }
    return this.http.get<ProductFamily[]>(`${this.apiUrl}/families`, { params });
  }

  createFamily(data: { supplierId: string; name: string; code: string; categoryIds: string[] }): Observable<ProductFamily> {
    return this.http.post<ProductFamily>(`${this.apiUrl}/families`, data);
  }

  updateFamily(id: string, data: { supplierId: string; name: string; code: string; categoryIds: string[] }): Observable<ProductFamily> {
    return this.http.put<ProductFamily>(`${this.apiUrl}/families/${id}`, data);
  }

  deleteFamily(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/families/${id}`);
  }
}
