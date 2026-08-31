import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProductCategory, ProductFamily, ProductSubfamily } from '../models/catalog.model';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private apiUrl = '/api/supplier/catalog';

  constructor(private http: HttpClient) {}

  listCategories(supplierId: number): Observable<ProductCategory[]> {
    return this.http.get<ProductCategory[]>(`${this.apiUrl}/categories`, {
      params: { supplierId: supplierId.toString() }
    });
  }

  createCategory(data: { supplierId: number; name: string; code: string }): Observable<ProductCategory> {
    return this.http.post<ProductCategory>(`${this.apiUrl}/categories`, {
      supplierId: data.supplierId, name: data.name, code: data.code
    });
  }

  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/categories/${id}`);
  }

  listFamilies(supplierId: number, categoryId?: number): Observable<ProductFamily[]> {
    let params = new HttpParams().set('supplierId', supplierId.toString());
    if (categoryId != null) {
      params = params.set('categoryId', categoryId.toString());
    }
    return this.http.get<ProductFamily[]>(`${this.apiUrl}/families`, { params });
  }

  createFamily(data: { supplierId: number; categoryId: number; name: string; code: string }): Observable<ProductFamily> {
    return this.http.post<ProductFamily>(`${this.apiUrl}/families`, {
      supplierId: data.supplierId, categoryId: data.categoryId, name: data.name, code: data.code
    });
  }

  deleteFamily(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/families/${id}`);
  }

  listSubfamilies(supplierId: number, familyId?: number): Observable<ProductSubfamily[]> {
    let params = new HttpParams().set('supplierId', supplierId.toString());
    if (familyId != null) {
      params = params.set('familyId', familyId.toString());
    }
    return this.http.get<ProductSubfamily[]>(`${this.apiUrl}/subfamilies`, { params });
  }

  createSubfamily(data: { supplierId: number; familyId: number; name: string; code: string }): Observable<ProductSubfamily> {
    return this.http.post<ProductSubfamily>(`${this.apiUrl}/subfamilies`, {
      supplierId: data.supplierId, familyId: data.familyId, name: data.name, code: data.code
    });
  }

  deleteSubfamily(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/subfamilies/${id}`);
  }
}
