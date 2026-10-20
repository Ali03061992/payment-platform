// @ts-nocheck
import { TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { DashboardAdminComponent } from './dashboard-admin.component';
import { LoginService } from '../../services/login.service';
import { UserService } from '../../services/user.service';
import { OrganizationService } from '../../services/organization.service';
import { PaymentService } from '../../services/payment.service';
import { OrderService } from '../../services/order.service';
import { DisputeService } from '../../services/dispute.service';
import { TranslateStubPipe, translateServiceProvider } from '../../testing/translate-stubs';

describe('DashboardAdminComponent', () => {
  let loginService: jasmine.SpyObj<LoginService>;
  let userService: jasmine.SpyObj<UserService>;
  let organizationService: jasmine.SpyObj<OrganizationService>;
  let paymentService: jasmine.SpyObj<PaymentService>;
  let orderService: jasmine.SpyObj<OrderService>;
  let disputeService: jasmine.SpyObj<DisputeService>;

  beforeEach(async () => {
    const loginSpy = jasmine.createSpyObj('LoginService', ['getCurrentUser']);
    const userSpy = jasmine.createSpyObj('UserService', ['list']);
    const orgSpy = jasmine.createSpyObj('OrganizationService', ['getStats', 'listRelations']);
    const paymentSpy = jasmine.createSpyObj('PaymentService', ['getStats']);
    const orderSpy = jasmine.createSpyObj('OrderService', ['listPaged', 'list']);
    const disputeSpy = jasmine.createSpyObj('DisputeService', ['list']);
    loginSpy.getCurrentUser.and.returnValue({ firstName: 'Ali' } as any);
    userSpy.list.and.returnValue(of([]));
    orgSpy.getStats.and.returnValue(of({ suppliers: 0, shops: 0 } as any));
    orgSpy.listRelations.and.returnValue(of([]));
    paymentSpy.getStats.and.returnValue(of({ total: 0, pending: 0, confirmed: 0, rejected: 0, cancelled: 0 } as any));
    orderSpy.listPaged.and.returnValue(of({ items: [], totalElements: 0, totalPages: 0, number: 0 } as any));
    disputeSpy.list.and.returnValue(of([]));
    await TestBed.configureTestingModule({
      declarations: [DashboardAdminComponent, TranslateStubPipe],
      providers: [provideHttpClient(), translateServiceProvider(), { provide: LoginService, useValue: loginSpy }, { provide: UserService, useValue: userSpy }, { provide: OrganizationService, useValue: orgSpy }, { provide: PaymentService, useValue: paymentSpy }, { provide: OrderService, useValue: orderSpy }, { provide: DisputeService, useValue: disputeSpy }],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
    loginService = TestBed.inject(LoginService) as jasmine.SpyObj<LoginService>;
    userService = TestBed.inject(UserService) as jasmine.SpyObj<UserService>;
    organizationService = TestBed.inject(OrganizationService) as jasmine.SpyObj<OrganizationService>;
    paymentService = TestBed.inject(PaymentService) as jasmine.SpyObj<PaymentService>;
    orderService = TestBed.inject(OrderService) as jasmine.SpyObj<OrderService>;
    disputeService = TestBed.inject(DisputeService) as jasmine.SpyObj<DisputeService>;
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(DashboardAdminComponent);
    expect(fixture.componentInstance).toBeDefined();
  });

  it('should use totalElements as exact order total (not page length)', () => {
    orderService.listPaged.and.returnValue(of({ items: [{ id: '1' }], totalElements: 1523, totalPages: 1523, number: 0 } as any));
    const fixture = TestBed.createComponent(DashboardAdminComponent);
    const component = fixture.componentInstance;
    component.loadStats();
    expect(orderService.listPaged).toHaveBeenCalledWith(0, 1);
    expect(component.recentOrdersCount).toBe(1523);
  });

  it('should load open disputes and active relations', () => {
    disputeService.list.and.returnValue(of([{ id: 'd1' }, { id: 'd2' }] as any));
    organizationService.listRelations.and.returnValue(of([{ id: 'r1' }] as any));
    const fixture = TestBed.createComponent(DashboardAdminComponent);
    const component = fixture.componentInstance;
    component.loadStats();
    expect(disputeService.list).toHaveBeenCalledWith('OPEN');
    expect(component.openDisputesCount).toBe(2);
    expect(component.relationsCount).toBe(1);
    expect(component.loading).toBeFalse();
  });

  it('should stop loading on partial error', () => {
    orderService.listPaged.and.returnValue(throwError(() => new Error('boom')));
    disputeService.list.and.returnValue(throwError(() => new Error('boom')));
    const fixture = TestBed.createComponent(DashboardAdminComponent);
    const component = fixture.componentInstance;
    component.loadStats();
    expect(component.loading).toBeFalse();
  });
});
