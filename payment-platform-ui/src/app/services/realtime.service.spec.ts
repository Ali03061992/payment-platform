// @ts-nocheck
/**
 * Tests du pont SSE -> invalidation du store.
 */
import { Subject } from 'rxjs';
import { RealtimeService } from './realtime.service';

describe('RealtimeService', () => {
  let notifications$: Subject<unknown>;
  let dataChanged$: Subject<void>;
  let notifications: { realtimeNotifications$: unknown; dataChanged$: unknown };
  let store: jasmine.SpyObj<{ invalidateAll: () => void }>;
  let service: RealtimeService;

  beforeEach(() => {
    notifications$ = new Subject();
    dataChanged$ = new Subject();
    notifications = {
      realtimeNotifications$: notifications$.asObservable(),
      dataChanged$: dataChanged$.asObservable()
    };
    store = jasmine.createSpyObj('DataStoreService', ['invalidateAll']);
    service = new RealtimeService(notifications as any, store as any);
  });

  it('should invalidate data on each realtime notification', () => {
    service.start();
    notifications$.next({ type: 'ORDER_CONFIRMED' });
    notifications$.next({ type: 'PAYMENT_CREATED' });
    expect(store.invalidateAll).toHaveBeenCalledTimes(2);
  });

  it('should invalidate data on a global data-changed signal', () => {
    service.start();
    dataChanged$.next();
    expect(store.invalidateAll).toHaveBeenCalledTimes(1);
  });

  it('should ignore notifications after stop', () => {
    service.start();
    service.stop();
    notifications$.next({ type: 'ORDER_CONFIRMED' });
    expect(store.invalidateAll).not.toHaveBeenCalled();
  });

  it('should not subscribe twice', () => {
    service.start();
    service.start();
    notifications$.next({ type: 'ORDER_CONFIRMED' });
    expect(store.invalidateAll).toHaveBeenCalledTimes(1);
  });
});
