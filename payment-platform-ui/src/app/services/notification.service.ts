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
  private unreadCountPollingSub?: Subscription;
  private lastUnreadCount = 0;

  notifications$ = this.notificationsSubject.asObservable();
  unreadCount$ = this.unreadCountSubject.asObservable();

  constructor(private http: HttpClient) {}

  startPolling(intervalMs = 30000): void {
    this.stopPolling();
    this.fetchNotifications().subscribe();
    this.fetchUnreadCount().subscribe();

    this.pollingSub = interval(intervalMs).pipe(
      switchMap(() => this.fetchNotifications())
    ).subscribe();
    this.unreadCountPollingSub = interval(intervalMs).pipe(
      switchMap(() => this.fetchUnreadCount())
    ).subscribe();
  }

  stopPolling(): void {
    this.pollingSub?.unsubscribe();
    this.unreadCountPollingSub?.unsubscribe();
  }

  fetchNotifications(): Observable<Notification[]> {
    return this.http.get<Notification[]>(this.apiUrl).pipe(
      tap(list => this.notificationsSubject.next(list || []))
    );
  }

  fetchUnreadCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.apiUrl}/unread-count`).pipe(
      tap(res => {
        const newCount = res.count || 0;
        // Show browser notification if count increased
        if (newCount > this.lastUnreadCount && this.lastUnreadCount > 0) {
          this.showBrowserNotification();
        }
        this.lastUnreadCount = newCount;
        this.unreadCountSubject.next(newCount);
      })
    );
  }

  markAsRead(id: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/read`, {});
  }

  markAllAsRead(): Observable<{ updated: number }> {
    return this.http.post<{ updated: number }>(`${this.apiUrl}/read-all`, {});
  }

  // Browser Notification API
  requestPermission(): Promise<NotificationPermission> {
    if (!('Notification' in window)) {
      return Promise.resolve('denied');
    }
    return Notification.requestPermission();
  }

  getPermissionStatus(): NotificationPermission | 'unsupported' {
    if (!('Notification' in window)) {
      return 'unsupported';
    }
    return Notification.permission;
  }

  showBrowserNotification(): void {
    if (!('Notification' in window)) return;

    if (Notification.permission === 'granted') {
      const notifications = this.notificationsSubject.getValue();
      if (notifications.length > 0) {
        const latest = notifications[0];
        new window.Notification('Payment Platform', {
          body: latest.message,
          icon: 'assets/icons/icon-192x192.png',
          tag: 'payment-notification',
          renotify: true
        });
      }
    }
  }
}
