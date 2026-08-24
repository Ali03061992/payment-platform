import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Payment, PaymentStats, CreatePaymentRequest, RejectPaymentRequest } from '../models/payment.model';

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
}
