import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BalanceEntry, BalanceSummary } from '../models/balance.model';

@Injectable({ providedIn: 'root' })
export class BalanceService {
  private apiUrl = '/api/balances';

  constructor(private http: HttpClient) {}

  listBySupplier(supplierId: string): Observable<BalanceSummary[]> {
    return this.http.get<BalanceSummary[]>(`${this.apiUrl}/supplier/${supplierId}`);
  }

  listByShop(shopId: string): Observable<BalanceSummary[]> {
    return this.http.get<BalanceSummary[]>(`${this.apiUrl}/shop/${shopId}`);
  }

  getHistory(supplierId: string, shopId: string): Observable<BalanceEntry[]> {
    return this.http.get<BalanceEntry[]>(`${this.apiUrl}/supplier/${supplierId}/shop/${shopId}`);
  }

  adjust(data: { supplierId: string; shopId: string; amount: number; reason: string }): Observable<BalanceEntry> {
    return this.http.post<BalanceEntry>(`${this.apiUrl}/adjust`, data);
  }
}
