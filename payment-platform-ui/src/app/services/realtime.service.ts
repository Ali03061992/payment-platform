import { Injectable, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { DataStoreService } from '../core/data-store.service';
import { NotificationService } from './notification.service';

/**
 * Relie le flux SSE des notifications à l'invalidation du store : dès qu'un
 * événement métier arrive, les pages actives revalident leurs données sans
 * rechargement manuel.
 */
@Injectable({ providedIn: 'root' })
export class RealtimeService implements OnDestroy {
  private subscription = new Subscription();
  private started = false;

  constructor(
    private notifications: NotificationService,
    private store: DataStoreService
  ) {}

  start(): void {
    if (this.started) return;
    this.started = true;
    this.subscription.add(
      this.notifications.realtimeNotifications$.subscribe(() => this.store.invalidateAll())
    );
    this.subscription.add(
      this.notifications.dataChanged$.subscribe(() => this.store.invalidateAll())
    );
  }

  stop(): void {
    this.subscription.unsubscribe();
    this.subscription = new Subscription();
    this.started = false;
  }

  ngOnDestroy(): void {
    this.stop();
  }
}
