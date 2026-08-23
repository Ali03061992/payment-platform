import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { LoginRequest, LoginResponse, User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class LoginService {
  private apiUrl = '/api/auth';

  constructor(private http: HttpClient) {}

  login(data: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, data).pipe(
      tap(res => {
        localStorage.setItem('token', res.accessToken);
      })
    );
  }

  getMe(): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/me`);
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('token');
  }

  getCurrentUser(): User | null {
    const userJson = localStorage.getItem('user');
    return userJson ? JSON.parse(userJson) : null;
  }

  hasRole(...roles: string[]): boolean {
    const user = this.getCurrentUser();
    if (!user) return false;
    return user.roles.some(r => roles.includes(r));
  }
}
