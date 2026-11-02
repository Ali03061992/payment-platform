import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AddDisputeMessageRequest,
  CreateDisputeRequest,
  Dispute,
  DisputePage,
  DisputeQuery
} from '../models/dispute.model';

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

  transition(disputeId: string, status: string): Observable<Dispute> {
    return this.http.post<Dispute>(`${this.apiUrl}/${disputeId}/transition`, { status });
  }

  updatePriority(disputeId: string, priority: string): Observable<Dispute> {
    return this.http.patch<Dispute>(`${this.apiUrl}/${disputeId}/priority`, { priority });
  }

  assign(disputeId: string, assigneeId?: string): Observable<Dispute> {
    return this.http.post<Dispute>(`${this.apiUrl}/${disputeId}/assign`,
      assigneeId ? { assigneeId } : {});
  }

  addNote(disputeId: string, content: string): Observable<Dispute> {
    return this.http.post<Dispute>(`${this.apiUrl}/${disputeId}/notes`, { content });
  }

  uploadAttachment(disputeId: string, file: File): Observable<Dispute> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<Dispute>(`${this.apiUrl}/${disputeId}/attachments`, formData);
  }

  downloadAttachment(disputeId: string, attachmentId: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${disputeId}/attachments/${attachmentId}`,
      { responseType: 'blob' });
  }

  list(status?: string): Observable<Dispute[]> {
    const url = status ? `${this.apiUrl}?status=${status}` : this.apiUrl;
    return this.http.get<Dispute[]>(url);
  }

  listPaged(query: DisputeQuery = {}): Observable<DisputePage> {
    let params = new HttpParams();
    if (query.status) params = params.set('status', query.status);
    if (query.priority) params = params.set('priority', query.priority);
    if (query.search) params = params.set('search', query.search);
    params = params.set('page', String(query.page ?? 0));
    params = params.set('size', String(query.size ?? 20));
    return this.http.get<DisputePage>(`${this.apiUrl}/paged`, { params });
  }
}
