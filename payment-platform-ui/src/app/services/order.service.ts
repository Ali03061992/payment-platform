import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Order, CreateOrderRequest, UpdateOrderRequest, OrderComment } from '../models/order.model';
import { ToastService } from './toast.service';
import { filenameFromDisposition, saveBlob } from '../core/file-download';

/**
 * Service HTTP des commandes (cycle de vie, livraisons, commentaires, exports).
 * Normalise les enveloppes paginées et télécharge factures et CSV.
 */
@Injectable({ providedIn: 'root' })
export class OrderService {
  private apiUrl = '/api/orders';

  constructor(private http: HttpClient, private toast: ToastService) {}

  /** Télécharge la facture PDF d'une commande et la sauvegarde localement. */
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

  /** Crée une commande. */
  create(data: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(this.apiUrl, data);
  }

  /** Met à jour une commande existante. */
  update(id: string, data: UpdateOrderRequest): Observable<Order> {
    return this.http.put<Order>(`${this.apiUrl}/${id}`, data);
  }

  /** Liste les commandes visibles, en normalisant les enveloppes paginées. */
  list(): Observable<Order[]> {
    return new Observable<Order[]>(observer => {
      this.http.get<any>(this.apiUrl).subscribe({
        next: (res: any) => {
          if (Array.isArray(res)) observer.next(res as Order[]);
          else if (res && Array.isArray(res.items)) observer.next(res.items as Order[]);
          else if (res && Array.isArray(res.content)) observer.next(res.content as Order[]);
          else if (res && Array.isArray(res.data)) observer.next(res.data as Order[]);
          else observer.next([]);
          observer.complete();
        },
        error: (err) => observer.error(err)
      });
    });
  }

  /** Récupère une commande par identifiant. */
  getById(id: string): Observable<Order> {
    return this.http.get<Order>(`${this.apiUrl}/${id}`);
  }

  /** Recherche des commandes par mot-clé (20 résultats normalisés). */
  search(query: string): Observable<Order[]> {
    return new Observable<Order[]>(observer => {
      this.http.get<any>(`${this.apiUrl}/search?q=${encodeURIComponent(query)}&size=20`).subscribe({
        next: (res: any) => {
          if (Array.isArray(res)) observer.next(res as Order[]);
          else if (res && Array.isArray(res.items)) observer.next(res.items as Order[]);
          else if (res && Array.isArray(res.content)) observer.next(res.content as Order[]);
          else if (res && Array.isArray(res.data)) observer.next(res.data as Order[]);
          else observer.next([]);
          observer.complete();
        },
        error: (err) => observer.error(err)
      });
    });
  }

  /** Récupère une commande par référence métier. */
  getByReference(reference: string): Observable<Order> {
    return this.http.get<Order>(`${this.apiUrl}/reference/${reference}`);
  }

  /** Confirme une commande côté fournisseur. */
  confirm(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/confirm`, {});
  }

  /** Passe une commande en préparation côté fournisseur. */
  prepare(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/prepare`, {});
  }

  /** Marque une commande prête pour la livraison. */
  readyForDelivery(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/ready`, {});
  }

  /** Assigne un livreur à une commande avec date prévisionnelle optionnelle. */
  assignDelivery(id: string, agentId: string, plannedDeliveryDate?: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/assign-delivery`, { agentId, plannedDeliveryDate });
  }

  /** Confirme la date de livraison d'une commande. */
  confirmDelivery(id: string, confirmedDate: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/confirm-delivery`, { confirmedDate });
  }

  /** Déclare une commande livrée à son destinataire. */
  deliver(id: string, receivedBy: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/deliver`, { receivedBy });
  }

  /** Accepte une commande livrée côté boutique. */
  accept(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/accept`, {});
  }

  /** Accepte une commande ASAP (déclenche le paiement automatique côté serveur). */
  acceptAsap(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/accept-asap`, {});
  }

  /** Annule une commande. */
  cancel(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/cancel`, {});
  }

  /** Rejette une commande livrée côté boutique. */
  reject(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/reject`, {});
  }

  /** Rejette une livraison côté fournisseur avec motif éventuel. */
  deliveryReject(id: string, reason?: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/delivery-reject`, { reason });
  }

  /** Accepte ou refuse une livraison assignée au livreur connecté. */
  acceptDelivery(id: string, accepted: boolean, reason?: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/accept-delivery`, { accepted, reason });
  }

  /** Liste les livraisons visibles, éventuellement filtrées par livreur. */
  listDeliveries(agentId?: string): Observable<Order[]> {
    return new Observable<Order[]>(observer => {
      let url = `${this.apiUrl}/deliveries`;
      if (agentId) url += `?agentId=${agentId}`;
      this.http.get<any>(url).subscribe({
        next: (res: any) => {
          if (Array.isArray(res)) observer.next(res as Order[]);
          else if (res && Array.isArray(res.items)) observer.next(res.items as Order[]);
          else if (res && Array.isArray(res.content)) observer.next(res.content as Order[]);
          else if (res && Array.isArray(res.data)) observer.next(res.data as Order[]);
          else observer.next([]);
          observer.complete();
        },
        error: (err) => observer.error(err)
      });
    });
  }

  /** Liste les agents d'une boutique pour le destinataire de livraison. */
  getShopAgents(shopId: string): Observable<{id: string, name: string, roles?: string}[]> {
    return this.http.get<{id: string, name: string, roles?: string}[]>(`${this.apiUrl}/shop-agents?shopId=${shopId}`);
  }

  /** Exporte les commandes filtrées en CSV et sauvegarde le fichier reçu. */
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
          'commandes.csv'
        );
        saveBlob(res.body, filename);
      },
      error: () => this.toast.error('Erreur lors de l\'export CSV')
    });
  }

  /** Liste les livraisons de l'utilisateur connecté (admin ou livreur). */
  myDeliveries(): Observable<Order[]> {
    return new Observable<Order[]>(observer => {
      this.http.get<any>(`${this.apiUrl}/my-deliveries`).subscribe({
        next: (res: any) => {
          if (Array.isArray(res)) observer.next(res as Order[]);
          else if (res && Array.isArray(res.items)) observer.next(res.items as Order[]);
          else if (res && Array.isArray(res.content)) observer.next(res.content as Order[]);
          else if (res && Array.isArray(res.data)) observer.next(res.data as Order[]);
          else observer.next([]);
          observer.complete();
        },
        error: (err) => observer.error(err)
      });
    });
  }

  /** Liste les commentaires d'une commande. */
  getComments(orderId: string): Observable<OrderComment[]> {
    return this.http.get<OrderComment[]>(`${this.apiUrl}/${orderId}/comments`);
  }

  /** Ajoute un commentaire à une commande. */
  addComment(orderId: string, content: string): Observable<OrderComment> {
    return this.http.post<OrderComment>(`${this.apiUrl}/${orderId}/comments`, { content });
  }

  /** Récupère les commandes les plus récentes (limite paramétrable). */
  getRecent(limit: number = 5): Observable<Order[]> {
    return new Observable<Order[]>(observer => {
      this.http.get<any>(`${this.apiUrl}/recent?limit=${limit}`).subscribe({
        next: (res: any) => {
          if (Array.isArray(res)) observer.next(res as Order[]);
          else if (res && Array.isArray(res.items)) observer.next(res.items as Order[]);
          else if (res && Array.isArray(res.content)) observer.next(res.content as Order[]);
          else if (res && Array.isArray(res.data)) observer.next(res.data as Order[]);
          else observer.next([]);
          observer.complete();
        },
        error: (err) => observer.error(err)
      });
    });
  }

  /** Recrée une commande à l'identique à partir d'une commande existante. */
  reorder(orderId: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${orderId}/reorder`, {});
  }

  /** Met à jour la position GPS du livreur pour une commande. */
  updateLocation(orderId: string, latitude: number, longitude: number, estimatedArrival?: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${orderId}/location`, { latitude, longitude, estimatedArrival });
  }
}
