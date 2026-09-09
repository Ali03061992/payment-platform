import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SearchPaymentsRequest {
  shopId?: string;
  supplierId?: string;
  createdBy?: string;
  status?: string;
  from?: string;
  to?: string;
  page: number;
  size: number;
}

export interface PaymentSearchResult {
  id: string;
  reference: string;
  shopId: string;
  shopName: string;
  supplierId: string;
  supplierName: string;
  createdBy: string;
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
