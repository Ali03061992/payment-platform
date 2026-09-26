import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { User } from '../models/user.model';

/**
 * Service HTTP des utilisateurs (liste filtrée, détail, activation, création).
 * Normalise les enveloppes paginées retournées par l'API.
 */
@Injectable({ providedIn: 'root' })
export class UserService {
  private apiUrl = '/api/users';

  constructor(private http: HttpClient) {}

  /** Liste les utilisateurs avec filtres optionnels (organisation, rôle, statut). */
  list(organizationId?: string, role?: string, status?: string, page = 0, size = 100): Observable<User[]> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (organizationId) params = params.set('organizationId', organizationId.toString());
    if (role) params = params.set('role', role);
    if (status) params = params.set('statusFilter', status);
    return this.http.get<User[] | { items: User[] }>(this.apiUrl, { params }).pipe(
      map((res: User[] | { items?: User[]; content?: User[]; data?: User[] }) => {
        if (Array.isArray(res)) return res;
        if (res && Array.isArray((res as any).items)) return (res as any).items;
        if (res && Array.isArray((res as any).content)) return (res as any).content;
        if (res && Array.isArray((res as any).data)) return (res as any).data;
        return [];
      })
    );
  }

  /** Récupère un utilisateur par identifiant. */
  getById(id: string): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/${id}`);
  }

  /** Crée un utilisateur (workflow admin). */
  create(data: any): Observable<User> {
    return this.http.post<User>(this.apiUrl, data);
  }

  /** Active un compte utilisateur en attente de validation. */
  activate(id: string): Observable<User> {
    return this.http.patch<User>(`${this.apiUrl}/${id}/activate`, {});
  }

  /** Désactive un compte utilisateur. */
  disable(id: string): Observable<User> {
    return this.http.patch<User>(`${this.apiUrl}/${id}/disable`, {});
  }
}
