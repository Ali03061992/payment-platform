import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Product, ProductCreateRequest, ProductUpdateRequest, StockMovement, StockMovementRequest } from '../models/stock.model';

@Injectable({ providedIn: 'root' })
export class StockService {
  private apiUrl = '/api/suppliers';

  constructor(private http: HttpClient) {}

  private getSupplierId(): string {
    const userJson = sessionStorage.getItem('user');
    if (userJson) {
      const user = JSON.parse(userJson);
      return user.organizationId || '';
    }
    return '';
  }

  getLowStockAlerts(): Observable<Product[]> {
    const supplierId = this.getSupplierId();
    return this.http.get<Product[]>(`${this.apiUrl}/${supplierId}/low-stock-alerts`);
  }

  getProducts(status?: string): Observable<Product[]> {
    const supplierId = this.getSupplierId();
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<Product[]>(`${this.apiUrl}/${supplierId}/products`, { params });
  }

  getProductsBySupplier(supplierId: string, status?: string): Observable<Product[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<Product[]>(`${this.apiUrl}/${supplierId}/products`, { params });
  }

  getProduct(id: string): Observable<Product> {
    const supplierId = this.getSupplierId();
    return this.http.get<Product>(`${this.apiUrl}/${supplierId}/products/${id}`);
  }

  createProduct(data: ProductCreateRequest): Observable<Product> {
    const supplierId = this.getSupplierId();
    return this.http.post<Product>(`${this.apiUrl}/${supplierId}/products`, data);
  }

  updateProduct(id: string, data: ProductUpdateRequest): Observable<Product> {
    const supplierId = this.getSupplierId();
    return this.http.patch<Product>(`${this.apiUrl}/${supplierId}/products/${id}`, data);
  }

  deleteProduct(id: string): Observable<Product> {
    const supplierId = this.getSupplierId();
    return this.http.patch<Product>(`${this.apiUrl}/${supplierId}/products/${id}/deactivate`, { status: 'INACTIVE' });
  }

  uploadProductImage(productId: string, file: File): Observable<Product> {
    const supplierId = this.getSupplierId();
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Product>(`${this.apiUrl}/${supplierId}/products/${productId}/image`, formData);
  }

  getStockMovements(productId?: string): Observable<StockMovement[]> {
    const supplierId = this.getSupplierId();
    let params = new HttpParams();
    if (productId) params = params.set('productId', productId.toString());
    return this.http.get<StockMovement[]>(`${this.apiUrl}/${supplierId}/movements`, { params });
  }

  createStockMovement(productId: string, data: StockMovementRequest): Observable<StockMovement> {
    const supplierId = this.getSupplierId();
    return this.http.post<StockMovement>(`${this.apiUrl}/${supplierId}/movements`, { ...data, productId });
  }
}
