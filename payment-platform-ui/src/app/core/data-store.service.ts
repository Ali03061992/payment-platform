import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpRequest, HttpResponse } from '@angular/common/http';
import { BehaviorSubject, Observable, filter } from 'rxjs';

interface CacheEntry {
  key: string;
  request: HttpRequest<unknown>;
  next: HttpHandler;
  response: BehaviorSubject<HttpResponse<unknown> | null>;
  lastFetched: number;
  inFlight: boolean;
  hasValue: boolean;
}

/**
 * Store mémoire des GET JSON : sert immédiatement la dernière valeur connue
 * (stale-while-revalidate), déduplique les appels simultanés et rafraîchit les
 * pages actives à intervalle court. L'invalidation SSE est déclenchée par
 * RealtimeService.
 */
@Injectable({ providedIn: 'root' })
export class DataStoreService {
  /** Durée de fraîcheur avant revalidation en arrière-plan. */
  static readonly TTL_MS = 20_000;
  private static readonly REFRESH_DEBOUNCE_MS = 350;
  private static readonly MAX_ENTRIES = 120;

  private readonly entries = new Map<string, CacheEntry>();
  private readonly pendingRefresh = new Set<CacheEntry>();
  private refreshTimer: ReturnType<typeof setInterval> | null = null;
  private debounceTimer: ReturnType<typeof setTimeout> | null = null;

  get(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const key = request.urlWithParams;
    let entry = this.entries.get(key);

    if (!entry) {
      entry = {
        key,
        request,
        next,
        response: new BehaviorSubject<HttpResponse<unknown> | null>(null),
        lastFetched: 0,
        inFlight: false,
        hasValue: false
      };
      this.entries.set(key, entry);
      this.evictOverflow();
      this.fetch(entry);
      this.ensureRefreshTimer();
    } else if (entry.hasValue && Date.now() - entry.lastFetched >= DataStoreService.TTL_MS) {
      this.fetch(entry);
    } else if (!entry.hasValue && !entry.inFlight) {
      this.fetch(entry);
    }

    return new Observable<HttpEvent<unknown>>((subscriber) => {
      const subscription = entry!.response
        .pipe(filter((event): event is HttpResponse<unknown> => event !== null))
        .subscribe(subscriber);
      return () => subscription.unsubscribe();
    });
  }

  /** Marque toutes les entrées comme périmées et rafraîchit celles affichées. */
  invalidateAll(): void {
    for (const entry of this.entries.values()) {
      entry.lastFetched = 0;
      if (entry.response.observed) {
        this.pendingRefresh.add(entry);
      }
    }
    this.schedulePendingRefresh();
  }

  /** Vide complètement le store (changement de session). */
  reset(): void {
    for (const entry of this.entries.values()) {
      entry.response.complete();
    }
    this.entries.clear();
    this.pendingRefresh.clear();
    if (this.debounceTimer) {
      clearTimeout(this.debounceTimer);
      this.debounceTimer = null;
    }
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer);
      this.refreshTimer = null;
    }
  }

  private fetch(entry: CacheEntry): void {
    if (entry.inFlight) return;
    entry.inFlight = true;

    entry.next.handle(entry.request).subscribe({
      next: (event) => {
        if (event instanceof HttpResponse && this.entries.has(entry.key)) {
          entry.lastFetched = Date.now();
          entry.hasValue = true;
          entry.response.next(event);
        }
      },
      error: (error) => {
        entry.inFlight = false;
        entry.lastFetched = Date.now();
        if (!entry.hasValue && this.entries.has(entry.key)) {
          this.entries.delete(entry.key);
          entry.response.error(error);
        }
      },
      complete: () => {
        entry.inFlight = false;
      }
    });
  }

  private schedulePendingRefresh(): void {
    if (this.debounceTimer || this.pendingRefresh.size === 0) return;
    this.debounceTimer = setTimeout(() => {
      this.debounceTimer = null;
      const toRefresh = Array.from(this.pendingRefresh);
      this.pendingRefresh.clear();
      for (const entry of toRefresh) {
        if (this.entries.has(entry.key) && entry.response.observed) {
          this.fetch(entry);
        }
      }
    }, DataStoreService.REFRESH_DEBOUNCE_MS);
  }

  private ensureRefreshTimer(): void {
    if (this.refreshTimer) return;
    this.refreshTimer = setInterval(() => {
      const now = Date.now();
      for (const entry of this.entries.values()) {
        if (entry.response.observed && entry.hasValue
            && now - entry.lastFetched >= DataStoreService.TTL_MS) {
          this.fetch(entry);
        }
      }
    }, DataStoreService.TTL_MS);
  }

  private evictOverflow(): void {
    if (this.entries.size <= DataStoreService.MAX_ENTRIES) return;
    const removable = Array.from(this.entries.values())
      .filter((entry) => !entry.response.observed)
      .sort((a, b) => a.lastFetched - b.lastFetched);
    for (const entry of removable) {
      if (this.entries.size <= DataStoreService.MAX_ENTRIES) break;
      this.entries.delete(entry.key);
      entry.response.complete();
    }
  }
}
