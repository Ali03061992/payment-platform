import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Organization, OrganizationStats, SupplierShopRelation, CreateRelationRequest } from '../models/organization.model';

/**
 * Service HTTP des organisations (fournisseurs, boutiques, relations, statistiques).
 * Normalise les enveloppes paginées retournées par l'admin.
 */
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

  /** Liste paginée des fournisseurs. */
  listSuppliers(page = 0, size = 100): Observable<Organization[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Organization[] | { items: Organization[] }>(`${this.apiUrl}/suppliers`, { params })
      .pipe(map(OrganizationService.unwrap<Organization>));
  }

  /** Liste paginée des boutiques. */
  listShops(page = 0, size = 100): Observable<Organization[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Organization[] | { items: Organization[] }>(`${this.apiUrl}/shops`, { params })
      .pipe(map(OrganizationService.unwrap<Organization>));
  }

  /** Récupère une organisation par identifiant. */
  getById(id: string): Observable<Organization> {
    return this.http.get<Organization>(`${this.apiUrl}/suppliers/${id}`);
  }

  /** Crée un fournisseur. */
  createSupplier(name: string): Observable<Organization> {
    return this.http.post<Organization>(`${this.apiUrl}/suppliers`, { name });
  }

  /** Crée une boutique. */
  createShop(name: string): Observable<Organization> {
    return this.http.post<Organization>(`${this.apiUrl}/shops`, { name });
  }

  /** Active une organisation (fournisseur ou boutique selon le type). */
  activate(id: string, type: string): Observable<Organization> {
    const path = type === 'SUPPLIER' ? 'suppliers' : 'shops';
    return this.http.patch<Organization>(`${this.apiUrl}/${path}/${id}/activate`, {});
  }

  /** Désactive une organisation (fournisseur ou boutique selon le type). */
  disable(id: string, type: string): Observable<Organization> {
    const path = type === 'SUPPLIER' ? 'suppliers' : 'shops';
    return this.http.patch<Organization>(`${this.apiUrl}/${path}/${id}/disable`, {});
  }

  /** Récupère les statistiques globales des organisations. */
  getStats(): Observable<OrganizationStats> {
    return this.http.get<OrganizationStats>(`${this.apiUrl}/stats`);
  }

  /** Liste toutes les relations fournisseur-boutique. */
  listRelations(): Observable<SupplierShopRelation[]> {
    return this.http.get<SupplierShopRelation[]>('/api/admin/supplier-shop-relations');
  }

  /** Liste les relations d'une boutique donnée. */
  listRelationsByShop(shopId: string): Observable<SupplierShopRelation[]> {
    return this.http.get<SupplierShopRelation[]>(`/api/admin/supplier-shop-relations/shop/${shopId}`);
  }

  /** Liste les relations d'un fournisseur donné. */
  listRelationsBySupplier(supplierId: string): Observable<SupplierShopRelation[]> {
    return this.http.get<SupplierShopRelation[]>(`/api/admin/supplier-shop-relations/supplier/${supplierId}`);
  }

  /** Crée une relation fournisseur-boutique. */
  createRelation(data: CreateRelationRequest): Observable<SupplierShopRelation> {
    return this.http.post<SupplierShopRelation>('/api/admin/supplier-shop-relations', data);
  }

  /** Désactive une relation fournisseur-boutique. */
  deactivateRelation(id: string): Observable<void> {
    return this.http.delete<void>(`/api/admin/supplier-shop-relations/${id}`);
  }

  /** Liste paginée des utilisateurs (via l'endpoint admin). */
  listUsers(page = 0, size = 100): Observable<any[]> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<any[] | { items: any[] }>('/api/users', { params })
      .pipe(map(OrganizationService.unwrap<any>));
  }
}
