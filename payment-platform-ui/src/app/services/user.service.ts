import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private apiUrl = '/api/users';

  constructor(private http: HttpClient) {}

  list(organizationId?: number, role?: string, status?: string): Observable<User[]> {
    let params = new HttpParams();
    if (organizationId) params = params.set('organizationId', organizationId.toString());
    if (role) params = params.set('role', role);
    if (status) params = params.set('statusFilter', status);
    return this.http.get<User[]>(this.apiUrl, { params });
  }

  getById(id: number): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/${id}`);
  }

  create(data: any): Observable<User> {
    return this.http.post<User>(this.apiUrl, data);
  }

  activate(id: number): Observable<User> {
    return this.http.patch<User>(`${this.apiUrl}/${id}/activate`, {});
  }

  disable(id: number): Observable<User> {
    return this.http.patch<User>(`${this.apiUrl}/${id}/disable`, {});
  }
}
