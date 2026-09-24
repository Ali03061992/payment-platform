import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Dispute, CreateDisputeRequest, AddDisputeMessageRequest } from '../models/dispute.model';

@Injectable({ providedIn: 'root' })
export class DisputeService {
  private apiUrl = '/api/disputes';

  constructor(private http: HttpClient) {}

  create(data: CreateDisputeRequest): Observable<Dispute> {
    return this.http.post<Dispute>(this.apiUrl, data);
  }

  getByOrder(orderId: string): Observable<Dispute[]> {
    return this.http.get<Dispute[]>(`${this.apiUrl}/order/${orderId}`);
  }

  getById(id: string): Observable<Dispute> {
    return this.http.get<Dispute>(`${this.apiUrl}/${id}`);
  }

  addMessage(disputeId: string, data: AddDisputeMessageRequest): Observable<Dispute> {
    return this.http.post<Dispute>(`${this.apiUrl}/${disputeId}/messages`, data);
  }

  resolve(disputeId: string, status: string = 'RESOLVED'): Observable<Dispute> {
    return this.http.post<Dispute>(`${this.apiUrl}/${disputeId}/resolve`, { status });
  }

  list(status?: string): Observable<Dispute[]> {
    const url = status ? `${this.apiUrl}?status=${status}` : this.apiUrl;
    return this.http.get<Dispute[]>(url);
  }
}
