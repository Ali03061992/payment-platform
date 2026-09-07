import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Product, ProductCreateRequest, ProductUpdateRequest, StockMovement, StockMovementRequest } from '../models/stock.model';

@Injectable({ providedIn: 'root' })
export class StockService {
  private apiUrl = '/api/suppliers';

  constructor(private http: HttpClient) {}

  private getSupplierId(): number {
    const userJson = sessionStorage.getItem('user');
    if (userJson) {
      const user = JSON.parse(userJson);
      return user.organizationId || 0;
    }
    return 0;
  }

  getProducts(status?: string): Observable<Product[]> {
    const supplierId = this.getSupplierId();
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<Product[]>(`${this.apiUrl}/${supplierId}/products`, { params });
  }

  getProduct(id: number): Observable<Product> {
    const supplierId = this.getSupplierId();
    return this.http.get<Product>(`${this.apiUrl}/${supplierId}/products/${id}`);
  }

  createProduct(data: ProductCreateRequest): Observable<Product> {
    const supplierId = this.getSupplierId();
    return this.http.post<Product>(`${this.apiUrl}/${supplierId}/products`, data);
  }

  updateProduct(id: number, data: ProductUpdateRequest): Observable<Product> {
    const supplierId = this.getSupplierId();
    return this.http.patch<Product>(`${this.apiUrl}/${supplierId}/products/${id}`, data);
  }

  deleteProduct(id: number): Observable<Product> {
    const supplierId = this.getSupplierId();
    return this.http.patch<Product>(`${this.apiUrl}/${supplierId}/products/${id}/deactivate`, { status: 'INACTIVE' });
  }

  getStockMovements(productId?: number): Observable<StockMovement[]> {
    const supplierId = this.getSupplierId();
    let params = new HttpParams();
    if (productId) params = params.set('productId', productId.toString());
    return this.http.get<StockMovement[]>(`${this.apiUrl}/${supplierId}/movements`, { params });
  }

  createStockMovement(productId: number, data: StockMovementRequest): Observable<StockMovement> {
    const supplierId = this.getSupplierId();
    return this.http.post<StockMovement>(`${this.apiUrl}/${supplierId}/movements`, { ...data, productId });
  }
}
