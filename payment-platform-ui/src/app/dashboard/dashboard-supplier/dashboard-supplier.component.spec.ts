// @ts-nocheck
import { TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { DashboardSupplierComponent } from './dashboard-supplier.component';
import { LoginService } from '../../services/login.service';
import { StockService } from '../../services/stock.service';
import { OrderService } from '../../services/order.service';
import { PaymentService } from '../../services/payment.service';
import { BalanceService } from '../../services/balance.service';
import { TranslateStubPipe, translateServiceProvider } from '../../testing/translate-stubs';

describe('DashboardSupplierComponent', () => {
  let loginService: jasmine.SpyObj<LoginService>;
  let stockService: jasmine.SpyObj<StockService>;
  let orderService: jasmine.SpyObj<OrderService>;
  let paymentService: jasmine.SpyObj<PaymentService>;
  let balanceService: jasmine.SpyObj<BalanceService>;

  beforeEach(async () => {
    const loginSpy = jasmine.createSpyObj('LoginService', ['getCurrentUser']);
    const stockSpy = jasmine.createSpyObj('StockService', ['getProducts', 'getLowStockAlerts']);
    const orderSpy = jasmine.createSpyObj('OrderService', ['listPaged', 'list']);
    const paymentSpy = jasmine.createSpyObj('PaymentService', ['getStats']);
    const balanceSpy = jasmine.createSpyObj('BalanceService', ['listBySupplier']);
    loginSpy.getCurrentUser.and.returnValue({ firstName: 'Ali', organizationId: 'sup-1' } as any);
    stockSpy.getProducts.and.returnValue(of([]));
    stockSpy.getLowStockAlerts.and.returnValue(of([]));
    orderSpy.listPaged.and.returnValue(of({ items: [], totalElements: 0, totalPages: 0, number: 0 } as any));
    paymentSpy.getStats.and.returnValue(of({ total: 0, pending: 0, confirmed: 0, rejected: 0, cancelled: 0 } as any));
    balanceSpy.listBySupplier.and.returnValue(of([]));
    await TestBed.configureTestingModule({
      declarations: [DashboardSupplierComponent, TranslateStubPipe],
      providers: [provideHttpClient(), translateServiceProvider(), { provide: LoginService, useValue: loginSpy }, { provide: StockService, useValue: stockSpy }, { provide: OrderService, useValue: orderSpy }, { provide: PaymentService, useValue: paymentSpy }, { provide: BalanceService, useValue: balanceSpy }],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
    loginService = TestBed.inject(LoginService) as jasmine.SpyObj<LoginService>;
    stockService = TestBed.inject(StockService) as jasmine.SpyObj<StockService>;
    orderService = TestBed.inject(OrderService) as jasmine.SpyObj<OrderService>;
    paymentService = TestBed.inject(PaymentService) as jasmine.SpyObj<PaymentService>;
    balanceService = TestBed.inject(BalanceService) as jasmine.SpyObj<BalanceService>;
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(DashboardSupplierComponent);
    expect(fixture.componentInstance).toBeDefined();
  });

  it('should use exact total and count open orders with real statuses (DRAFT, no PENDING)', () => {
    orderService.listPaged.and.returnValue(of({
      items: [
        { id: '1', status: 'DRAFT' },
        { id: '2', status: 'CONFIRMED' },
        { id: '3', status: 'PREPARING' },
        { id: '4', status: 'DELIVERED' },
        { id: '5', status: 'PENDING' }
      ],
      totalElements: 242,
      totalPages: 3,
      number: 0
    } as any));
    const fixture = TestBed.createComponent(DashboardSupplierComponent);
    const component = fixture.componentInstance;
    component.loadStats();
    expect(component.totalOrders).toBe(242);
    expect(component.pendingOrdersCount).toBe(3);
    expect(component.recentOrders.length).toBe(5);
    expect(component.loading).toBeFalse();
  });

  it('should compute outstanding by shops', () => {
    balanceService.listBySupplier.and.returnValue(of([
      { shopId: 'b1', shopName: 'B1', remainingDue: 300 },
      { shopId: 'b2', shopName: 'B2', remainingDue: 50 }
    ] as any));
    const fixture = TestBed.createComponent(DashboardSupplierComponent);
    const component = fixture.componentInstance;
    component.loadStats();
    expect(balanceService.listBySupplier).toHaveBeenCalledWith('sup-1');
    expect(component.totalDueByShops).toBe(350);
    expect(component.activeShopCount).toBe(2);
  });

  it('should stop loading on partial error', () => {
    orderService.listPaged.and.returnValue(throwError(() => new Error('boom')));
    const fixture = TestBed.createComponent(DashboardSupplierComponent);
    const component = fixture.componentInstance;
    component.loadStats();
    expect(component.loading).toBeFalse();
  });
});
