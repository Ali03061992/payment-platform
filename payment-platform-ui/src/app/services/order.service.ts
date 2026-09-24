import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Order, CreateOrderRequest, UpdateOrderRequest, OrderComment } from '../models/order.model';
import { ToastService } from './toast.service';
import { filenameFromDisposition, saveBlob } from '../core/file-download';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private apiUrl = '/api/orders';

  constructor(private http: HttpClient, private toast: ToastService) {}

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

  create(data: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(this.apiUrl, data);
  }

  update(id: string, data: UpdateOrderRequest): Observable<Order> {
    return this.http.put<Order>(`${this.apiUrl}/${id}`, data);
  }

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

  getById(id: string): Observable<Order> {
    return this.http.get<Order>(`${this.apiUrl}/${id}`);
  }

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

  getByReference(reference: string): Observable<Order> {
    return this.http.get<Order>(`${this.apiUrl}/reference/${reference}`);
  }

  confirm(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/confirm`, {});
  }

  prepare(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/prepare`, {});
  }

  readyForDelivery(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/ready`, {});
  }

  assignDelivery(id: string, agentId: string, plannedDeliveryDate?: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/assign-delivery`, { agentId, plannedDeliveryDate });
  }

  confirmDelivery(id: string, confirmedDate: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/confirm-delivery`, { confirmedDate });
  }

  deliver(id: string, receivedBy: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/deliver`, { receivedBy });
  }

  accept(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/accept`, {});
  }

  acceptAsap(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/accept-asap`, {});
  }

  cancel(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/cancel`, {});
  }

  reject(id: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/reject`, {});
  }

  deliveryReject(id: string, reason?: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/delivery-reject`, { reason });
  }

  acceptDelivery(id: string, accepted: boolean, reason?: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/accept-delivery`, { accepted, reason });
  }

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

  getShopAgents(shopId: string): Observable<{id: string, name: string}[]> {
    return this.http.get<{id: string, name: string}[]>(`${this.apiUrl}/shop-agents?shopId=${shopId}`);
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
          'commandes.csv'
        );
        saveBlob(res.body, filename);
      },
      error: () => this.toast.error('Erreur lors de l\'export CSV')
    });
  }

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

  getComments(orderId: string): Observable<OrderComment[]> {
    return this.http.get<OrderComment[]>(`${this.apiUrl}/${orderId}/comments`);
  }

  addComment(orderId: string, content: string): Observable<OrderComment> {
    return this.http.post<OrderComment>(`${this.apiUrl}/${orderId}/comments`, { content });
  }

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

  reorder(orderId: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${orderId}/reorder`, {});
  }

  updateLocation(orderId: string, latitude: number, longitude: number, estimatedArrival?: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${orderId}/location`, { latitude, longitude, estimatedArrival });
  }
}
