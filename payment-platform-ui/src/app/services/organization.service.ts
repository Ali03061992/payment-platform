import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Organization, OrganizationStats, SupplierShopRelation, CreateRelationRequest } from '../models/organization.model';

@Injectable({ providedIn: 'root' })
export class OrganizationService {
  private apiUrl = '/api/admin';

  constructor(private http: HttpClient) {}

  private static unwrap<T>(res: T[] | { items?: T[]; content?: T[]; data?: T[] }): T[] {
    if (Array.isArray(res)) return res;
    if (res && Array.isArray((res as any).items)) return (res as any).items;
    if (res && Array.isArray((res as any).content)) return (res as any).content;
    if (res && Array.isArray((res as any).data)) return (res as any).data;
    return [];
  }

  listSuppliers(page = 0, size = 100): Observable<Organization[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Organization[] | { items: Organization[] }>(`${this.apiUrl}/suppliers`, { params })
      .pipe(map(OrganizationService.unwrap<Organization>));
  }

  listShops(page = 0, size = 100): Observable<Organization[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Organization[] | { items: Organization[] }>(`${this.apiUrl}/shops`, { params })
      .pipe(map(OrganizationService.unwrap<Organization>));
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

  listUsers(page = 0, size = 100): Observable<any[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<any[] | { items: any[] }>('/api/users', { params })
      .pipe(map(OrganizationService.unwrap<any>));
  }
}
