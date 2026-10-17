import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { SwPush } from '@angular/service-worker';
import { Observable, from, map, switchMap } from 'rxjs';
import { NotificationPreferences } from '../models/passkey.model';

interface PushSubscriptionListEntry {
  id: string;
  endpoint: string;
  userAgent: string;
  createdAt: string;
  lastUsedAt: string;
}

/**
 * Web Push (PWA) — abonnement via ngsw/SwPush et préférences de notification.
 * Opt-in explicite : aucune demande de permission n'est faite automatiquement.
 * Coexiste avec le canal FCM existant (PushNotificationService).
 */
@Injectable({ providedIn: 'root' })
export class WebPushService {

  private apiUrl = '/api/push';

  constructor(private http: HttpClient, private swPush: SwPush) {}

  /** True si le navigateur supporte Service Worker + Push Manager. */
  isSupported(): boolean {
    return 'serviceWorker' in navigator && 'PushManager' in window;
  }

  /** Clé publique VAPID (base64url) à injecter dans PushManager.subscribe. */
  getVapidPublicKey(): Observable<string> {
    return this.http.get<{ publicKey: string }>(`${this.apiUrl}/vapid-public-key`)
      .pipe(map((res) => res.publicKey));
  }

  /** Abonnement courant (null si absent). */
  currentSubscription(): Observable<PushSubscription | null> {
    return from(
      navigator.serviceWorker.ready.then(
        (reg) => reg.pushManager.getSubscription(),
        () => null
      )
    );
  }

  /** Souscrit au push (demande de permission navigateur) et enregistre l'abonnement côté serveur. */
  enable(): Observable<void> {
    return this.getVapidPublicKey().pipe(
      switchMap((serverPublicKey) =>
        from(this.swPush.requestSubscription({ serverPublicKey }))
      ),
      switchMap((subscription) => {
        const json = subscription.toJSON();
        return this.http.post<void>(`${this.apiUrl}/subscriptions`, {
          endpoint: json.endpoint ?? '',
          p256dh: json.keys?.['p256dh'] ?? '',
          auth: json.keys?.['auth'] ?? '',
          userAgent: navigator.userAgent.slice(0, 512)
        });
      })
    );
  }

  /** Désabonne le navigateur puis retire l'abonnement côté serveur. */
  disable(): Observable<void> {
    return this.currentSubscription().pipe(
      switchMap((subscription) => {
        if (!subscription) {
          return this.deleteByEndpoint(null);
        }
        const endpoint = subscription.endpoint;
        return this.deleteByEndpoint(endpoint).pipe(
          switchMap(() => from(subscription.unsubscribe().then(() => undefined)))
        );
      })
    );
  }

  private deleteByEndpoint(endpoint: string | null): Observable<void> {
    let params = new HttpParams();
    if (endpoint) {
      params = params.set('endpoint', endpoint);
    }
    return this.http.delete<void>(`${this.apiUrl}/subscriptions`, { params });
  }

  /** Liste les appareils abonnés au push. */
  listSubscriptions(): Observable<PushSubscriptionListEntry[]> {
    return this.http.get<PushSubscriptionListEntry[]>(`${this.apiUrl}/subscriptions`);
  }

  /** Préférences de notification de l'utilisateur. */
  getPreferences(): Observable<NotificationPreferences> {
    return this.http.get<NotificationPreferences>(`${this.apiUrl}/preferences`);
  }

  /** Met à jour les préférences de notification. */
  updatePreferences(preferences: NotificationPreferences): Observable<NotificationPreferences> {
    return this.http.put<NotificationPreferences>(`${this.apiUrl}/preferences`, preferences);
  }
}
