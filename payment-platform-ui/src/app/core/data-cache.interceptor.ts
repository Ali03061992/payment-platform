import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpResponse
} from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { DataStoreService } from './data-store.service';

/**
 * Cache HTTP des GET JSON + invalidation après mutation. Le store conserve la
 * dernière réponse et la sert instantanément à la navigation suivante, puis
 * revalide en arrière-plan (stale-while-revalidate).
 */
@Injectable()
export class DataCacheInterceptor implements HttpInterceptor {

  constructor(private store: DataStoreService) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (request.method !== 'GET') {
      return next.handle(request).pipe(
        tap((event) => {
          if (event instanceof HttpResponse) {
            if (this.isAuthUrl(request.url)) {
              this.store.reset();
            } else {
              this.store.invalidateAll();
            }
          }
        })
      );
    }

    if (!this.isCacheable(request)) {
      return next.handle(request);
    }
    return this.store.get(request, next);
  }

  private isCacheable(request: HttpRequest<unknown>): boolean {
    if (request.responseType !== 'json') return false;
    if (!request.url.startsWith('/api/')) return false;
    if (this.isAuthUrl(request.url)) return false;
    if (request.url.includes('/api/notifications')) return false;
    if (request.url.includes('/stream')) return false;
    if (request.headers.has('x-skip-cache')) return false;
    return true;
  }

  private isAuthUrl(url: string): boolean {
    return url.includes('/api/auth/');
  }
}
