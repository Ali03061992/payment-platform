import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable, interval, Subscription, of } from 'rxjs';
import { switchMap, tap, map, catchError } from 'rxjs/operators';
import { Notification, NotificationPage } from '../models/notification.model';
import { NotificationSoundService } from './notification-sound.service';
import { LoginService } from './login.service';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private apiUrl = '/api/notifications';
  private notificationsSubject = new BehaviorSubject<Notification[]>([]);
  private unreadCountSubject = new BehaviorSubject<number>(0);
  private pollingSub?: Subscription;
  private unreadCountPollingSub?: Subscription;
  private lastUnreadCount = 0;
  private eventSource: EventSource | null = null;
  private reconnectTimer: any;

  notifications$ = this.notificationsSubject.asObservable();
  unreadCount$ = this.unreadCountSubject.asObservable();

  constructor(
    private http: HttpClient,
    private sound: NotificationSoundService,
    private login: LoginService
  ) {}

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

  startRealtime(): void {
    this.stopRealtime();

    // Présence du token (même expiré) : ensureFreshToken le régénère ensuite
    // via le refresh single-flight (cookie HttpOnly).
    if (!this.login.getToken()) return;

    // Le token transporté en query string de l'EventSource ne passe PAS par
    // l'interceptor HTTP (pas de refresh silencieux sur 401) : sans fraîcheur
    // explicite, le SSE part en boucle 401 dès expiration (30 min) jusqu'au
    // prochain refresh de page. Le refresh est single-flight partagé avec
    // l'interceptor (cookie HttpOnly) — pas de rotation concurrente.
    this.ensureFreshToken().subscribe({
      next: () => this.openEventSource(),
      error: () => this.openEventSource()
    });
  }

  /** Rafraîchit le token s'il est expiré/quasi expiré ; termine toujours. */
  private ensureFreshToken(): Observable<unknown> {
    if (!this.isTokenExpired()) return of(null);
    return this.login.refresh().pipe(
      map(() => null),
      catchError(() => of(null))
    );
  }

  /** True si le JWT access est expiré (ou indéchiffrable) à moins de 5 s près. */
  private isTokenExpired(): boolean {
    const token = this.login.getToken();
    if (!token) return true;
    try {
      const payload = JSON.parse(atob(token.split('.')[1] || ''));
      if (!payload.exp) return false;
      return payload.exp * 1000 < Date.now() + 5000;
    } catch {
      return false;
    }
  }

  private openEventSource(): void {
    const token = this.login.getToken();
    if (!token) return;

    const user = this.login.getCurrentUser();
    const params = new URLSearchParams();
    params.set('token', token);

    if (user?.organizationId) {
      params.set('orgId', user.organizationId);
    } else if (user?.id) {
      params.set('userId', user.id);
    }

    const url = `${this.apiUrl}/stream?${params.toString()}`;
    this.eventSource = new EventSource(url);

    this.eventSource.addEventListener('notification', (event: MessageEvent) => {
      try {
        const notification: Notification = JSON.parse(event.data);
        this.addNotification(notification);
      } catch (e) {
      }
    });

    this.eventSource.onerror = () => {
      this.stopRealtime();
      this.reconnectTimer = setTimeout(() => this.startRealtime(), 5000);
    };
  }

  stopRealtime(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  private addNotification(notification: Notification): void {
    const current = this.notificationsSubject.getValue();
    const exists = current.some(n => n.id === notification.id);
    if (!exists) {
      this.notificationsSubject.next([notification, ...current]);
      this.unreadCountSubject.next(this.unreadCountSubject.getValue() + 1);
      this.sound.play();
      this.showBrowserNotification(notification);
    }
  }

  fetchNotifications(): Observable<Notification[]> {
    return this.http.get<Notification[] | NotificationPage>(this.apiUrl).pipe(
      map(body => this.extractItems(body)),
      tap(list => this.notificationsSubject.next(list))
    );
  }

  private extractItems(body: Notification[] | NotificationPage | null | undefined): Notification[] {
    if (!body) return [];
    if (Array.isArray(body)) return body;
    if (Array.isArray((body as NotificationPage).items)) return (body as NotificationPage).items;
    return [];
  }

  fetchNotificationsPaged(page = 0, size = 20, type?: string): Observable<NotificationPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (type) {
      params = params.set('type', type);
    }
    return this.http.get<NotificationPage>(this.apiUrl, { params });
  }

  fetchUnreadCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.apiUrl}/unread-count`).pipe(
      tap(res => {
        const newCount = res.count || 0;
        if (newCount > this.lastUnreadCount && this.lastUnreadCount > 0) {
          this.showBrowserNotification();
        }
        this.lastUnreadCount = newCount;
        this.unreadCountSubject.next(newCount);
      })
    );
  }

  markAsRead(id: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/read`, {});
  }

  markAllAsRead(): Observable<{ updated: number }> {
    return this.http.post<{ updated: number }>(`${this.apiUrl}/read-all`, {});
  }

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

  showBrowserNotification(notif?: Notification): void {
    if (!('Notification' in window)) return;
    if (Notification.permission !== 'granted') return;

    const n = notif || this.notificationsSubject.getValue()[0];
    if (!n) return;

    const title = 'Payment Platform';
    const options = {
      body: n.message,
      icon: 'assets/icons/icon-192x192.png',
      badge: 'assets/icons/icon-72x72.png',
      tag: 'payment-notification',
      renotify: true
    } as NotificationOptions;

    // Chrome Android (onglet comme PWA) : le constructeur depuis la page
    // lève un TypeError (« Illegal constructor »). Seule voie mobile fiable :
    // ServiceWorkerRegistration.showNotification() quand un SW est actif.
    if ('serviceWorker' in navigator) {
      navigator.serviceWorker.getRegistration().then((reg) => {
        if (reg && reg.active) {
          reg.showNotification(title, options).catch(() => {});
          return;
        }
        this.showViaConstructor(title, options);
      }, () => this.showViaConstructor(title, options));
      return;
    }
    this.showViaConstructor(title, options);
  }

  /** Repli desktop/dev : constructeur natif, silencieux si refusé. */
  private showViaConstructor(title: string, options: NotificationOptions): void {
    try {
      new window.Notification(title, options);
    } catch {}
  }
}
