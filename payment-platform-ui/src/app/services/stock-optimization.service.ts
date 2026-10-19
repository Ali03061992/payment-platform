import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { StockOptimizationResponse } from '../models/stock-optimization.model';
import { LoginService } from './login.service';

@Injectable({ providedIn: 'root' })
export class StockOptimizationService {
  private apiUrl = '/api/suppliers';

  constructor(private http: HttpClient, private login: LoginService) {}

  private getSupplierId(): string {
    return this.login.getCurrentUser()?.organizationId || '';
  }

  optimize(): Observable<StockOptimizationResponse> {
    const sid = this.getSupplierId();
    return this.http.get<StockOptimizationResponse>(`${this.apiUrl}/${sid}/optimization`);
  }

  configure(leadTimeDays: number, orderingCost: number, holdingCostPercent: number): Observable<StockOptimizationResponse> {
    const sid = this.getSupplierId();
    return this.http.post<StockOptimizationResponse>(
      `${this.apiUrl}/${sid}/optimization/configure`,
      null,
      { params: { leadTimeDays, orderingCost, holdingCostPercent } }
    );
  }
}
