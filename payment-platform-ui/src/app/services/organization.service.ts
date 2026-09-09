import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Organization, OrganizationStats, SupplierShopRelation, CreateRelationRequest } from '../models/organization.model';

@Injectable({ providedIn: 'root' })
export class OrganizationService {
  private apiUrl = '/api/admin';

  constructor(private http: HttpClient) {}

  listSuppliers(): Observable<Organization[]> {
    return this.http.get<Organization[]>(`${this.apiUrl}/suppliers`);
  }

  listShops(): Observable<Organization[]> {
    return this.http.get<Organization[]>(`${this.apiUrl}/shops`);
  }

  getById(id: string): Observable<Organization> {
    return this.http.get<Organization>(`${this.apiUrl}/suppliers/${id}`);
  }

  createSupplier(name: string): Observable<Organization> {
    return this.http.post<Organization>(`${this.apiUrl}/suppliers`, { name });
  }

  createShop(name: string): Observable<Organization> {
    return this.http.post<Organization>(`${this.apiUrl}/shops`, { name });
  }

  activate(id: string, type: string): Observable<Organization> {
    const path = type === 'SUPPLIER' ? 'suppliers' : 'shops';
    return this.http.patch<Organization>(`${this.apiUrl}/${path}/${id}/activate`, {});
  }

  disable(id: string, type: string): Observable<Organization> {
    const path = type === 'SUPPLIER' ? 'suppliers' : 'shops';
    return this.http.patch<Organization>(`${this.apiUrl}/${path}/${id}/disable`, {});
  }

  getStats(): Observable<OrganizationStats> {
    return this.http.get<OrganizationStats>(`${this.apiUrl}/stats`);
  }

  listRelations(): Observable<SupplierShopRelation[]> {
    return this.http.get<SupplierShopRelation[]>('/api/admin/supplier-shop-relations');
  }

  listRelationsByShop(shopId: string): Observable<SupplierShopRelation[]> {
    return this.http.get<SupplierShopRelation[]>(`/api/admin/supplier-shop-relations/shop/${shopId}`);
  }

  listRelationsBySupplier(supplierId: string): Observable<SupplierShopRelation[]> {
    return this.http.get<SupplierShopRelation[]>(`/api/admin/supplier-shop-relations/supplier/${supplierId}`);
  }

  createRelation(data: CreateRelationRequest): Observable<SupplierShopRelation> {
    return this.http.post<SupplierShopRelation>('/api/admin/supplier-shop-relations', data);
  }

  deactivateRelation(id: string): Observable<void> {
    return this.http.delete<void>(`/api/admin/supplier-shop-relations/${id}`);
  }

  listUsers(): Observable<any[]> {
    return this.http.get<any[]>('/api/users');
  }
}
