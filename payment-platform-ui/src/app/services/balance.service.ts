import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BalanceEntry, BalanceSummary } from '../models/balance.model';

@Injectable({ providedIn: 'root' })
export class BalanceService {
  private apiUrl = '/api/balances';

  constructor(private http: HttpClient) {}

  listBySupplier(supplierId: number): Observable<BalanceSummary[]> {
    return this.http.get<BalanceSummary[]>(`${this.apiUrl}/supplier/${supplierId}`);
  }

  listByShop(shopId: number): Observable<BalanceSummary[]> {
    return this.http.get<BalanceSummary[]>(`${this.apiUrl}/shop/${shopId}`);
  }

  getHistory(supplierId: number, shopId: number): Observable<BalanceEntry[]> {
    return this.http.get<BalanceEntry[]>(`${this.apiUrl}/history`, {
      params: { supplierId: supplierId.toString(), shopId: shopId.toString() }
    });
  }

  adjust(data: { supplierId: number; shopId: number; amount: number; reason: string }): Observable<BalanceEntry> {
    return this.http.post<BalanceEntry>(`${this.apiUrl}/adjust`, data);
  }
}
