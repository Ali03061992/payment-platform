import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { initializeApp, FirebaseApp } from 'firebase/app';
import { getMessaging, getToken, onMessage, Messaging } from 'firebase/messaging';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PushNotificationService {
  private apiUrl = '/api/fcm-tokens';
  private fcmTokenSubject = new BehaviorSubject<string | null>(null);
  private firebaseApp!: FirebaseApp;
  private messaging: Messaging | null = null;

  fcmToken$ = this.fcmTokenSubject.asObservable();

  constructor(
    private http: HttpClient,
    private ngZone: NgZone
  ) {
    try {
      this.firebaseApp = initializeApp(environment.firebase);
      this.messaging = getMessaging(this.firebaseApp);
    } catch (error) {
      console.error('Failed to initialize Firebase', error);
    }
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
      console.error('Failed to get FCM token', error);
    }
    return null;
  }

  registerToken(token: string): Observable<void> {
    return this.http.post<void>(this.apiUrl, { token }).pipe(
      tap(() => console.log('FCM token registered')),
      catchError(err => {
        console.error('Failed to register FCM token', err);
        return of(undefined);
      })
    );
  }

  listenToMessages(): void {
    if (!this.messaging) return;

    this.ngZone.runOutsideAngular(() => {
      onMessage(this.messaging!, (payload) => {
        this.ngZone.run(() => {
          console.log('Push notification received:', payload);
        });
      });
    });
  }
}
