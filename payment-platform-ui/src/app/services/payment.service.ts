import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Payment, PaymentStats, CreatePaymentRequest, RejectPaymentRequest } from '../models/payment.model';
import { AgentPaymentSummary } from '../models/agent-payment.model';
import { ToastService } from './toast.service';
import { filenameFromDisposition, saveBlob } from '../core/file-download';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private apiUrl = '/api/payments';

  constructor(private http: HttpClient, private toast: ToastService) {}

  list(): Observable<Payment[]> {
    return new Observable<Payment[]>(observer => {
      this.http.get<any>(this.apiUrl).subscribe({
        next: (res: any) => {
          if (Array.isArray(res)) observer.next(res as Payment[]);
          else if (res && Array.isArray(res.items)) observer.next(res.items as Payment[]);
          else if (res && Array.isArray(res.content)) observer.next(res.content as Payment[]);
          else if (res && Array.isArray(res.data)) observer.next(res.data as Payment[]);
          else observer.next([]);
          observer.complete();
        },
        error: (err) => observer.error(err)
      });
    });
  }

  getById(id: string): Observable<Payment> {
    return this.http.get<Payment>(`${this.apiUrl}/${id}`);
  }

  getByReference(reference: string): Observable<Payment> {
    return this.http.get<Payment>(`${this.apiUrl}/reference/${reference}`);
  }

  create(data: CreatePaymentRequest): Observable<Payment> {
    return this.http.post<Payment>(this.apiUrl, data);
  }

  confirm(id: string): Observable<Payment> {
    return this.http.post<Payment>(`${this.apiUrl}/${id}/confirm`, {});
  }

  reject(id: string, data: RejectPaymentRequest): Observable<Payment> {
    return this.http.post<Payment>(`${this.apiUrl}/${id}/reject`, data);
  }

  cancel(id: string): Observable<Payment> {
    return this.http.post<Payment>(`${this.apiUrl}/${id}/cancel`, {});
  }

  downloadInvoice(id: string): void {
    this.http.get(`${this.apiUrl}/${id}/invoice`, { observe: 'response', responseType: 'blob' }).subscribe({
      next: (res) => {
        if (!res.body) {
          this.toast.error('Erreur lors du téléchargement de la facture');
          return;
        }
        const filename = filenameFromDisposition(
          res.headers.get('Content-Disposition'),
          `facture-${id}.pdf`
        );
        saveBlob(res.body, filename);
      },
      error: () => this.toast.error('Erreur lors du téléchargement de la facture')
    });
  }

  getStats(): Observable<PaymentStats> {
    return this.http.get<PaymentStats>(`${this.apiUrl}/stats`);
  }

  getAgentSummary(supplierId: string, from: string, to: string): Observable<AgentPaymentSummary[]> {
    const params = new HttpParams()
      .set('supplierId', supplierId.toString())
      .set('from', from)
      .set('to', to);
    return this.http.get<AgentPaymentSummary[]>(`${this.apiUrl}/agent-summary`, { params });
  }

  exportCsv(filters: { status?: string; dateFrom?: string; dateTo?: string }): void {
    let params = new URLSearchParams();
    if (filters.status) params.set('status', filters.status);
    if (filters.dateFrom) params.set('dateFrom', filters.dateFrom);
    if (filters.dateTo) params.set('dateTo', filters.dateTo);
    const qs = params.toString();
    this.http.get(`${this.apiUrl}/export/csv${qs ? '?' + qs : ''}`, { observe: 'response', responseType: 'blob' }).subscribe({
      next: (res) => {
        if (!res.body) {
          this.toast.error('Erreur lors de l\'export CSV');
          return;
        }
        const filename = filenameFromDisposition(
          res.headers.get('Content-Disposition'),
          'paiements.csv'
        );
        saveBlob(res.body, filename);
      },
      error: () => this.toast.error('Erreur lors de l\'export CSV')
    });
  }
}
