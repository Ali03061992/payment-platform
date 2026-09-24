import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuditLogPage } from '../models/audit-log.model';

@Injectable({ providedIn: 'root' })
export class AuditLogService {
  private apiUrl = '/api/admin/audit-logs';

  constructor(private http: HttpClient) {}

  list(params: {
    page?: number;
    size?: number;
    userId?: string;
    action?: string;
    dateFrom?: string;
    dateTo?: string;
  } = {}): Observable<AuditLogPage> {
    let httpParams = new HttpParams();
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
    if (params.userId) httpParams = httpParams.set('userId', params.userId);
    if (params.action) httpParams = httpParams.set('action', params.action);
    if (params.dateFrom) httpParams = httpParams.set('dateFrom', params.dateFrom);
    if (params.dateTo) httpParams = httpParams.set('dateTo', params.dateTo);
    return this.http.get<AuditLogPage>(this.apiUrl, { params: httpParams });
  }
}
