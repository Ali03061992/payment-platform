import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Payment, PaymentStats, CreatePaymentRequest, RejectPaymentRequest } from '../models/payment.model';
import { AgentPaymentSummary } from '../models/agent-payment.model';
import { ToastService } from './toast.service';
import { filenameFromDisposition, saveBlob } from '../core/file-download';

/**
 * Service HTTP des paiements (CRUD, validation, exports CSV et facture PDF).
 * Normalise les enveloppes paginées et porte la clé d'idempotence à la création.
 */
@Injectable({ providedIn: 'root' })
export class PaymentService {
  private apiUrl = '/api/payments';

  constructor(private http: HttpClient, private toast: ToastService) {}

  /** Liste les paiements visibles, en normalisant les enveloppes paginées. */
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

  /** Récupère un paiement par identifiant. */
  getById(id: string): Observable<Payment> {
    return this.http.get<Payment>(`${this.apiUrl}/${id}`);
  }

  /** Récupère un paiement par référence métier. */
  getByReference(reference: string): Observable<Payment> {
    return this.http.get<Payment>(`${this.apiUrl}/reference/${reference}`);
  }

  /** Crée un paiement en joignant une clé d'idempotence (générée si absente). */
  create(data: CreatePaymentRequest, idempotencyKey?: string): Observable<Payment> {
    const key = idempotencyKey ?? PaymentService.newIdempotencyKey();
    const headers = new HttpHeaders({ 'Idempotency-Key': key });
    return this.http.post<Payment>(this.apiUrl, data, { headers });
  }

  /** Génère une clé d'idempotence UUID v4 (crypto ou repli aléatoire). */
  static newIdempotencyKey(): string {
    try {
      const c = globalThis.crypto as unknown as { randomUUID?: () => string } | undefined;
      if (c?.randomUUID) return c.randomUUID();
    } catch { /* fallback ci-dessous */ }
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (ch) => {
      const r = Math.floor(Math.random() * 16);
      const v = ch === 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }

  /** Confirme un paiement côté fournisseur. */
  confirm(id: string): Observable<Payment> {
    return this.http.post<Payment>(`${this.apiUrl}/${id}/confirm`, {});
  }

  /** Rejette un paiement avec motif côté fournisseur. */
  reject(id: string, data: RejectPaymentRequest): Observable<Payment> {
    return this.http.post<Payment>(`${this.apiUrl}/${id}/reject`, data);
  }

  /** Annule un paiement en attente. */
  cancel(id: string): Observable<Payment> {
    return this.http.post<Payment>(`${this.apiUrl}/${id}/cancel`, {});
  }

  /** Télécharge la facture PDF d'un paiement confirmé et la sauvegarde localement. */
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

  /** Récupère les statistiques des paiements par statut. */
  getStats(): Observable<PaymentStats> {
    return this.http.get<PaymentStats>(`${this.apiUrl}/stats`);
  }

  /** Récupère la synthèse des paiements par agent sur une période. */
  getAgentSummary(supplierId: string, from: string, to: string): Observable<AgentPaymentSummary[]> {
    const params = new HttpParams()
      .set('supplierId', supplierId.toString())
      .set('from', from)
      .set('to', to);
    return this.http.get<AgentPaymentSummary[]>(`${this.apiUrl}/agent-summary`, { params });
  }

  /** Exporte les paiements filtrés en CSV et sauvegarde le fichier reçu. */
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
