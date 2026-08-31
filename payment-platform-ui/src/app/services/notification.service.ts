import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, interval, Subscription } from 'rxjs';
import { switchMap, tap } from 'rxjs/operators';
import { Notification } from '../models/notification.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private apiUrl = '/api/notifications';
  private notificationsSubject = new BehaviorSubject<Notification[]>([]);
  private unreadCountSubject = new BehaviorSubject<number>(0);
  private pollingSub?: Subscription;

  notifications$ = this.notificationsSubject.asObservable();
  unreadCount$ = this.unreadCountSubject.asObservable();

  constructor(private http: HttpClient) {}

  startPolling(intervalMs = 30000): void {
    this.fetchNotifications().subscribe();
    this.fetchUnreadCount().subscribe();

    this.pollingSub = interval(intervalMs).pipe(
      switchMap(() => this.fetchNotifications())
    ).subscribe();
    interval(intervalMs).pipe(
      switchMap(() => this.fetchUnreadCount())
    ).subscribe();
  }

  stopPolling(): void {
    this.pollingSub?.unsubscribe();
  }

  fetchNotifications(): Observable<Notification[]> {
    return this.http.get<Notification[]>(this.apiUrl).pipe(
      tap(list => this.notificationsSubject.next(list || []))
    );
  }

  fetchUnreadCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.apiUrl}/unread-count`).pipe(
      tap(res => this.unreadCountSubject.next(res.count || 0))
    );
  }

  markAsRead(id: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/read`, {});
  }

  markAllAsRead(): Observable<{ updated: number }> {
    return this.http.post<{ updated: number }>(`${this.apiUrl}/read-all`, {});
  }
}
