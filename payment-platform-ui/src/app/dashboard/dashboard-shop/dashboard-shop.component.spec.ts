// @ts-nocheck
import { TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { DashboardShopComponent } from './dashboard-shop.component';
import { LoginService } from '../../services/login.service';
import { OrderService } from '../../services/order.service';
import { PaymentService } from '../../services/payment.service';
import { BalanceService } from '../../services/balance.service';
import { QuoteService } from '../../services/quote.service';
import { TranslateStubPipe, translateServiceProvider } from '../../testing/translate-stubs';

describe('DashboardShopComponent', () => {
  let loginService: jasmine.SpyObj<LoginService>;
  let orderService: jasmine.SpyObj<OrderService>;
  let paymentService: jasmine.SpyObj<PaymentService>;
  let balanceService: jasmine.SpyObj<BalanceService>;
  let quoteService: jasmine.SpyObj<QuoteService>;

  beforeEach(async () => {
    const loginSpy = jasmine.createSpyObj('LoginService', ['getCurrentUser', 'hasRole']);
    loginSpy.hasRole.and.returnValue(true);
    const orderSpy = jasmine.createSpyObj('OrderService', ['getRecent', 'listPaged', 'list']);
    const paymentSpy = jasmine.createSpyObj('PaymentService', ['list', 'getStats']);
    const balanceSpy = jasmine.createSpyObj('BalanceService', ['listByShop']);
    const quoteSpy = jasmine.createSpyObj('QuoteService', ['myRequests']);
    loginSpy.getCurrentUser.and.returnValue({ firstName: 'Ali', organizationId: 'shop-1' } as any);
    orderSpy.getRecent.and.returnValue(of([]));
    orderSpy.listPaged.and.returnValue(of({ items: [], totalElements: 0, totalPages: 0, number: 0 } as any));
    paymentSpy.list.and.returnValue(of([]));
    paymentSpy.getStats.and.returnValue(of({ total: 0, pending: 0, confirmed: 0, rejected: 0, cancelled: 0 } as any));
    balanceSpy.listByShop.and.returnValue(of([]));
    quoteSpy.myRequests.and.returnValue(of([]));
    await TestBed.configureTestingModule({
      declarations: [DashboardShopComponent, TranslateStubPipe],
      providers: [provideHttpClient(), translateServiceProvider(), { provide: LoginService, useValue: loginSpy }, { provide: OrderService, useValue: orderSpy }, { provide: PaymentService, useValue: paymentSpy }, { provide: BalanceService, useValue: balanceSpy }, { provide: QuoteService, useValue: quoteSpy }],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
    loginService = TestBed.inject(LoginService) as jasmine.SpyObj<LoginService>;
    orderService = TestBed.inject(OrderService) as jasmine.SpyObj<OrderService>;
    paymentService = TestBed.inject(PaymentService) as jasmine.SpyObj<PaymentService>;
    balanceService = TestBed.inject(BalanceService) as jasmine.SpyObj<BalanceService>;
    quoteService = TestBed.inject(QuoteService) as jasmine.SpyObj<QuoteService>;
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(DashboardShopComponent);
    expect(fixture.componentInstance).toBeDefined();
  });

  it('should use totalElements as exact total and real order statuses', () => {
    orderService.listPaged.and.returnValue(of({
      items: [
        { id: '1', status: 'DRAFT' },
        { id: '2', status: 'CONFIRMED' },
        { id: '3', status: 'PREPARING' },
        { id: '4', status: 'READY_FOR_DELIVERY' },
        { id: '5', status: 'DELIVERY_ACCEPTED' },
        { id: '6', status: 'IN_DELIVERY' },
        { id: '7', status: 'DELIVERED' },
        { id: '8', status: 'ACCEPTED' },
        { id: '9', status: 'PENDING' },
        { id: '10', status: 'READY' },
        { id: '11', status: 'ASSIGNED' }
      ],
      totalElements: 137,
      totalPages: 2,
      number: 0
    } as any));
    const fixture = TestBed.createComponent(DashboardShopComponent);
    const component = fixture.componentInstance;
    component.loadStats();
    expect(component.totalOrders).toBe(137);
    expect(component.pendingOrdersCount).toBe(3);
    expect(component.inTransitCount).toBe(3);
    expect(component.deliveredOrdersCount).toBe(2);
    expect(component.loading).toBeFalse();
  });

  it('should compute outstanding to suppliers and pending quotes', () => {
    balanceService.listByShop.and.returnValue(of([
      { shopId: 's1', shopName: 'F1', remainingDue: 120.5 },
      { shopId: 's2', shopName: 'F2', remainingDue: 79.5 }
    ] as any));
    quoteService.myRequests.and.returnValue(of([
      { id: 'q1', status: 'QUOTED' },
      { id: 'q2', status: 'QUOTED' },
      { id: 'q3', status: 'PENDING' }
    ] as any));
    const fixture = TestBed.createComponent(DashboardShopComponent);
    const component = fixture.componentInstance;
    component.loadStats();
    expect(component.totalDueToSuppliers).toBe(200);
    expect(component.supplierCount).toBe(2);
    expect(component.pendingQuotesCount).toBe(2);
    expect(quoteService.myRequests).toHaveBeenCalled();
  });

  it('should skip balances query without organizationId', () => {
    loginService.getCurrentUser.and.returnValue({ firstName: 'Ali' } as any);
    const fixture = TestBed.createComponent(DashboardShopComponent);
    const component = fixture.componentInstance;
    component.ngOnInit();
    component.loadStats();
    expect(balanceService.listByShop).not.toHaveBeenCalled();
  });

  it('should stop loading on partial error', () => {
    orderService.listPaged.and.returnValue(throwError(() => new Error('boom')));
    const fixture = TestBed.createComponent(DashboardShopComponent);
    const component = fixture.componentInstance;
    component.loadStats();
    expect(component.loading).toBeFalse();
  });
});
