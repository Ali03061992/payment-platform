import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Order, CreateOrderRequest } from '../models/order.model';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private apiUrl = '/api/orders';

  constructor(private http: HttpClient) {}

  create(data: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(this.apiUrl, data);
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

  myDeliveries(): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.apiUrl}/my-deliveries`);
  }
}
