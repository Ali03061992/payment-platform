import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SearchPaymentsRequest {
  shopId?: number;
  supplierId?: number;
  createdBy?: number;
  status?: string;
  from?: string;
  to?: string;
  page: number;
  size: number;
}

export interface PaymentSearchResult {
  id: number;
  reference: string;
  shopId: number;
  shopName: string;
  supplierId: number;
  supplierName: string;
  createdBy: number;
  createdByName: string;
  amount: number;
  currency: string;
  status: string;
  rejectionReason: string;
  createdAt: string;
  updatedAt: string;
}

export interface SearchPaymentsResponse {
  payments: PaymentSearchResult[];
  total: number;
  page: number;
  size: number;
}

@Injectable({ providedIn: 'root' })
export class PaymentSearchService {
  private apiUrl = '/api/payments/search';

  constructor(private http: HttpClient) {}

  search(request: SearchPaymentsRequest): Observable<SearchPaymentsResponse> {
    return this.http.post<SearchPaymentsResponse>(this.apiUrl, request);
  }
}
