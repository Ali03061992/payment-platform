import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { initializeApp, FirebaseApp } from 'firebase/app';
import { getMessaging, getToken, onMessage, Messaging } from 'firebase/messaging';
import { environment } from '../../environments/environment';
import { NotificationSoundService } from './notification-sound.service';
import { BRAND } from '../core/brand.config';

@Injectable({ providedIn: 'root' })
export class PushNotificationService {
  private apiUrl = '/api/fcm-tokens';
  private static readonly TOKEN_KEY = 'pp-fcm-token';
  private fcmTokenSubject = new BehaviorSubject<string | null>(null);
  private firebaseApp!: FirebaseApp;
  private messaging: Messaging | null = null;

  fcmToken$ = this.fcmTokenSubject.asObservable();

  constructor(
    private http: HttpClient,
    private ngZone: NgZone,
    private router: Router,
    private sound: NotificationSoundService
  ) {
    // M7 : sans clés configurées (env), le push est désactivé proprement
    // au lieu d'initialiser Firebase avec des placeholders.
    if (!PushNotificationService.isConfigured()) {
      return;
    }
    try {
      this.firebaseApp = initializeApp(environment.firebase);
      this.messaging = getMessaging(this.firebaseApp);
    } catch (error) {
      this.messaging = null;
    }
  }

  static isConfigured(): boolean {
    const fb = environment.firebase;
    return !!fb.apiKey && !!fb.projectId && !!fb.messagingSenderId && !!fb.appId
      && !!environment.fcmVapidKey;
  }

  async requestPermissionAndGetToken(): Promise<string | null> {
    if (!('Notification' in window) || !this.messaging) {
      return null;
    }

    try {
      const permission = await Notification.requestPermission();
      if (permission !== 'granted') {
        return null;
      }

      const token = await getToken(this.messaging, {
        vapidKey: environment.fcmVapidKey
      });

      if (token) {
        this.fcmTokenSubject.next(token);
        this.registerToken(token).subscribe();
        return token;
      }
    } catch (error) {
    }
    return null;
  }

  registerToken(token: string): Observable<void> {
    try { localStorage.setItem(PushNotificationService.TOKEN_KEY, token); } catch {}
    return this.http.post<void>(this.apiUrl, { token }).pipe(
      catchError(err => {
        return of(undefined);
      })
    );
  }

  listenToMessages(): void {
    if (!this.messaging) return;

    this.ngZone.runOutsideAngular(() => {
      onMessage(this.messaging!, (payload) => {
        this.ngZone.run(() => {
          this.sound.play();
          try {
            if ('Notification' in window && Notification.permission === 'granted') {
              const title = (payload.notification?.title as string) || BRAND.name;
              const body = (payload.notification?.body as string) || '';
              const targetUrl = (payload.data as Record<string, string> | undefined)?.['url'];
              const notification = new window.Notification(title, {
                body,
                icon: 'assets/icons/icon-192x192.png',
                tag: 'payment-notification',
                renotify: true
              } as NotificationOptions);
              notification.onclick = () => {
                window.focus();
                if (!targetUrl) return;
                this.ngZone.run(() => {
                  if (targetUrl.startsWith('/')) {
                    void this.router.navigateByUrl(targetUrl);
                  } else {
                    window.open(targetUrl, '_blank', 'noopener');
                  }
                });
              };
            }
          } catch {}
        });
      });
    });
  }
}
