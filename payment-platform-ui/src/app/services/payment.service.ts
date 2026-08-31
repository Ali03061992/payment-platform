import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Payment, PaymentStats, CreatePaymentRequest, RejectPaymentRequest } from '../models/payment.model';
import { AgentPaymentSummary } from '../models/agent-payment.model';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private apiUrl = '/api/payments';

  constructor(private http: HttpClient) {}

  list(): Observable<Payment[]> {
    return this.http.get<Payment[]>(this.apiUrl);
  }

  getById(id: number): Observable<Payment> {
    return this.http.get<Payment>(`${this.apiUrl}/${id}`);
  }

  getByReference(reference: string): Observable<Payment> {
    return this.http.get<Payment>(`${this.apiUrl}/reference/${reference}`);
  }

  create(data: CreatePaymentRequest): Observable<Payment> {
    return this.http.post<Payment>(this.apiUrl, data);
  }

  confirm(id: number): Observable<Payment> {
    return this.http.post<Payment>(`${this.apiUrl}/${id}/confirm`, {});
  }

  reject(id: number, data: RejectPaymentRequest): Observable<Payment> {
    return this.http.post<Payment>(`${this.apiUrl}/${id}/reject`, data);
  }

  cancel(id: number): Observable<Payment> {
    return this.http.post<Payment>(`${this.apiUrl}/${id}/cancel`, {});
  }

  getStats(): Observable<PaymentStats> {
    return this.http.get<PaymentStats>(`${this.apiUrl}/stats`);
  }

  getAgentSummary(supplierId: number, from: string, to: string): Observable<AgentPaymentSummary[]> {
    const params = new HttpParams()
      .set('supplierId', supplierId.toString())
      .set('from', from)
      .set('to', to);
    return this.http.get<AgentPaymentSummary[]>(`${this.apiUrl}/agent-summary`, { params });
  }
}
