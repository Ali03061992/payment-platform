import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ReportGranularity, SupplierFinancialReport } from '../models/supplier-financial.model';

export interface SupplierFinancialQuery {
  from?: string;
  to?: string;
  granularity?: ReportGranularity;
}

@Injectable({ providedIn: 'root' })
export class ReportService {
  private apiUrl = '/api/reports';

  constructor(private http: HttpClient) {}

  getSupplierFinancialReport(query: SupplierFinancialQuery = {}): Observable<SupplierFinancialReport> {
    let params = new HttpParams();
    if (query.from) params = params.set('from', query.from);
    if (query.to) params = params.set('to', query.to);
    if (query.granularity) params = params.set('granularity', query.granularity);
    return this.http.get<SupplierFinancialReport>(`${this.apiUrl}/supplier-financial`, { params });
  }
}
