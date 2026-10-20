// @ts-nocheck
import { TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { DashboardAgentComponent } from './dashboard-agent.component';
import { LoginService } from '../../services/login.service';
import { OrderService } from '../../services/order.service';
import { TranslateStubPipe, translateServiceProvider } from '../../testing/translate-stubs';

describe('DashboardAgentComponent', () => {
  let loginService: jasmine.SpyObj<LoginService>;
  let orderService: jasmine.SpyObj<OrderService>;

  beforeEach(async () => {
    const loginSpy = jasmine.createSpyObj('LoginService', ['getCurrentUser']);
    const orderSpy = jasmine.createSpyObj('OrderService', ['myDeliveries']);
    loginSpy.getCurrentUser.and.returnValue({ firstName: 'Ali', organizationId: 'org-1' } as any);
    orderSpy.myDeliveries.and.returnValue(of([]));
    await TestBed.configureTestingModule({
      declarations: [DashboardAgentComponent, TranslateStubPipe],
      providers: [provideHttpClient(), translateServiceProvider(), { provide: LoginService, useValue: loginSpy }, { provide: OrderService, useValue: orderSpy }],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
    loginService = TestBed.inject(LoginService) as jasmine.SpyObj<LoginService>;
    orderService = TestBed.inject(OrderService) as jasmine.SpyObj<OrderService>;
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(DashboardAgentComponent);
    expect(fixture.componentInstance).toBeDefined();
  });

  it('should count real delivery statuses (legacy ASSIGNED/READY ignored)', () => {
    const today = new Date().toISOString().split('T')[0];
    orderService.myDeliveries.and.returnValue(of([
      { id: '1', status: 'READY_FOR_DELIVERY' },
      { id: '2', status: 'DELIVERY_ACCEPTED' },
      { id: '3', status: 'IN_DELIVERY' },
      { id: '4', status: 'DELIVERED', deliveredAt: `${today}T08:00:00Z` },
      { id: '5', status: 'DELIVERED', deliveredAt: '2020-01-01T08:00:00Z' },
      { id: '6', status: 'ASSIGNED' },
      { id: '7', status: 'READY' },
      { id: '8', status: 'CONFIRMED' }
    ] as any));
    const fixture = TestBed.createComponent(DashboardAgentComponent);
    const component = fixture.componentInstance;
    component.loadStats();
    expect(component.assignedCount).toBe(1);
    expect(component.inProgressCount).toBe(2);
    expect(component.completedTodayCount).toBe(1);
    expect(component.assignedOrders.length).toBe(1);
    expect(component.inProgressOrders.length).toBe(2);
    expect(component.loading).toBeFalse();
  });

  it('should stop loading on error', () => {
    orderService.myDeliveries.and.returnValue(throwError(() => new Error('boom')));
    const fixture = TestBed.createComponent(DashboardAgentComponent);
    const component = fixture.componentInstance;
    component.loadStats();
    expect(component.loading).toBeFalse();
  });

  it('should map real statuses in getStatusClass', () => {
    const fixture = TestBed.createComponent(DashboardAgentComponent);
    const component = fixture.componentInstance;
    expect(component.getStatusClass('READY_FOR_DELIVERY')).toBe('badge-warning');
    expect(component.getStatusClass('IN_DELIVERY')).toBe('badge-info');
    expect(component.getStatusClass('DELIVERED')).toBe('badge-success');
    expect(component.getStatusClass('UNKNOWN')).toBe('');
  });
});
