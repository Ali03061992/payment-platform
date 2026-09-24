import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SupplierFinancialReport } from '../models/supplier-financial.model';

@Injectable({ providedIn: 'root' })
export class ReportService {
  private apiUrl = '/api/reports';

  constructor(private http: HttpClient) {}

  getSupplierFinancialReport(): Observable<SupplierFinancialReport> {
    return this.http.get<SupplierFinancialReport>(`${this.apiUrl}/supplier-financial`);
  }
}
