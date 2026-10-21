import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { Order } from '../models/order.model';
import { QuoteRequest, SupplierQuote } from '../models/order.model';
import { ToastService } from './toast.service';
import { filenameFromDisposition, saveBlob } from '../core/file-download';

/**
 * Demandes de devis découplées des commandes : la boutique demande (zéro
 * remise), le fournisseur chiffre via l'application, la boutique accepte
 * (crée la commande) et télécharge chaque devis.
 */
@Injectable({ providedIn: 'root' })
export class QuoteService {
  private apiUrl = '/api/quotes';

  constructor(private http: HttpClient, private toast: ToastService, private translate: TranslateService) {}

  /** Crée une demande de devis (boutique, sans remise). */
  createRequest(supplierId: string, notes: string, items: { productId: string; quantity: number }[]): Observable<QuoteRequest> {
    return this.http.post<QuoteRequest>(`${this.apiUrl}/requests`, { supplierId, notes, items });
  }

  /** Demandes visibles (les siennes / celles reçues selon le rôle). */
  myRequests(): Observable<QuoteRequest[]> {
    return this.http.get<QuoteRequest[]>(`${this.apiUrl}/requests`);
  }

  /** Détail d'une demande + ses devis. */
  getRequest(id: string): Observable<QuoteRequest> {
    return this.http.get<QuoteRequest>(`${this.apiUrl}/requests/${id}`);
  }

  /** Détail d'une demande par sa référence (QR du devis). */
  getRequestByReference(reference: string): Observable<QuoteRequest> {
    return this.http.get<QuoteRequest>(`${this.apiUrl}/requests/reference/${encodeURIComponent(reference)}`);
  }

  /** Chiffre et envoie un devis versionné (fournisseur) : remises lignes en %. */
  sendQuote(requestId: string, items: { productId: string; quantity: number; discount: number }[]): Observable<SupplierQuote> {
    return this.http.post<SupplierQuote>(`${this.apiUrl}/requests/${requestId}/quotes`, { items });
  }

  /** Accepte un devis (boutique) : crée la commande confirmée. */
  acceptQuote(quoteId: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/quotes/${quoteId}/accept`, {});
  }

  /** Annule une demande (boutique). */
  cancelRequest(requestId: string): Observable<QuoteRequest> {
    return this.http.patch<QuoteRequest>(`${this.apiUrl}/requests/${requestId}/cancel`, {});
  }

  /** Télécharge un devis en PDF. */
  downloadQuotePdf(requestId: string, version: number, reference: string): void {
    this.http.get(`${this.apiUrl}/requests/${requestId}/quotes/${version}/pdf`, { observe: 'response', responseType: 'blob' }).subscribe({
      next: (res) => {
        if (!res.body) {
          this.toast.error(this.translate.instant('PAYMENTS.INVOICE_DOWNLOAD_ERROR'));
          return;
        }
        saveBlob(res.body, filenameFromDisposition(res.headers.get('Content-Disposition'), `devis-${reference}-v${version}.pdf`));
      },
      error: () => this.toast.error(this.translate.instant('PAYMENTS.INVOICE_DOWNLOAD_ERROR'))
    });
  }

  /** Parse les lignes figées (demande ou devis) pour affichage. */
  parseLines(linesJson: string): any[] {
    try {
      const parsed = JSON.parse(linesJson || '[]');
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }
}
