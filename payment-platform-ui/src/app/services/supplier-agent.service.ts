import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SupplierAgent {
  id: number;
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  status: string;
  organizationId: number;
}

export interface CreateAgentRequest {
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
}

@Injectable({ providedIn: 'root' })
export class SupplierAgentService {
  private apiUrl = '/api/suppliers';

  constructor(private http: HttpClient) {}

  listAgents(supplierId: number): Observable<SupplierAgent[]> {
    return this.http.get<SupplierAgent[]>(`${this.apiUrl}/${supplierId}/agents`);
  }

  createAgent(supplierId: number, data: CreateAgentRequest): Observable<SupplierAgent> {
    return this.http.post<SupplierAgent>(`${this.apiUrl}/${supplierId}/agents`, data);
  }

  updateAgent(supplierId: number, agentId: number, data: Partial<CreateAgentRequest>): Observable<SupplierAgent> {
    return this.http.patch<SupplierAgent>(`${this.apiUrl}/${supplierId}/agents/${agentId}`, data);
  }

  activateAgent(supplierId: number, agentId: number): Observable<SupplierAgent> {
    return this.http.patch<SupplierAgent>(`${this.apiUrl}/${supplierId}/agents/${agentId}/activate`, {});
  }

  disableAgent(supplierId: number, agentId: number): Observable<SupplierAgent> {
    return this.http.patch<SupplierAgent>(`${this.apiUrl}/${supplierId}/agents/${agentId}/disable`, {});
  }
}
