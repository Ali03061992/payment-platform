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

  getById(id: number): Observable<Order> {
    return this.http.get<Order>(`${this.apiUrl}/${id}`);
  }

  confirm(id: number): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/confirm`, {});
  }

  prepare(id: number): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/prepare`, {});
  }

  readyForDelivery(id: number): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/ready`, {});
  }

  assignDelivery(id: number, agentId: number): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/assign-delivery`, { agentId });
  }

  deliver(id: number, receivedBy: number): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/deliver`, { receivedBy });
  }

  accept(id: number): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/accept`, {});
  }

  acceptAsap(id: number): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/accept-asap`, {});
  }

  cancel(id: number): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/cancel`, {});
  }

  reject(id: number): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/reject`, {});
  }

  deliveryReject(id: number, reason?: string): Observable<Order> {
    return this.http.post<Order>(`${this.apiUrl}/${id}/delivery-reject`, { reason });
  }

  myDeliveries(): Observable<Order[]> {
    return this.http.get<Order[]>(`${this.apiUrl}/my-deliveries`);
  }
}
