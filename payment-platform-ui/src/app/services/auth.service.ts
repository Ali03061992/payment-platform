import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RegisterRequest, User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = '/api/auth';

  constructor(private http: HttpClient) {}

  register(data: RegisterRequest): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}/register`, data);
  }
}
