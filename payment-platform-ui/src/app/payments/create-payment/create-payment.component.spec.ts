// @ts-nocheck
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { CreatePaymentComponent } from './create-payment.component';
import { PaymentService } from '../../services/payment.service';
import { OrganizationService } from '../../services/organization.service';
import { ToastService } from '../../services/toast.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('CreatePaymentComponent', () => {
  let component: CreatePaymentComponent;
  let fixture: ComponentFixture<CreatePaymentComponent>;
  let router: jasmine.SpyObj<Router>;
  let paymentService: jasmine.SpyObj<PaymentService>;
  let orgService: jasmine.SpyObj<OrganizationService>;
  let toast: jasmine.SpyObj<ToastService>;

  beforeEach(() => {
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const psSpy = jasmine.createSpyObj('PaymentService', ['create']);
    const orgSpy = jasmine.createSpyObj('OrganizationService', ['listShops', 'listSuppliers', 'listRelationsByShop', 'getById']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    psSpy.create.and.returnValue(of({ id: 1 } as any));
    orgSpy.listShops.and.returnValue(of([]));
    orgSpy.listSuppliers.and.returnValue(of([]));
    orgSpy.listRelationsByShop.and.returnValue(of([]));
    orgSpy.getById.and.returnValue(of({ id: 1, name: 'Sup' } as any));
    sessionStorage.clear();

    TestBed.configureTestingModule({
    declarations: [CreatePaymentComponent],
    schemas: [NO_ERRORS_SCHEMA],
    imports: [],
    providers: [
        { provide: Router, useValue: routerSpy },
        { provide: PaymentService, useValue: psSpy },
        { provide: OrganizationService, useValue: orgSpy },
        { provide: ToastService, useValue: toastSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(CreatePaymentComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    paymentService = TestBed.inject(PaymentService) as jasmine.SpyObj<PaymentService>;
    orgService = TestBed.inject(OrganizationService) as jasmine.SpyObj<OrganizationService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  afterEach(() => sessionStorage.clear());

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have initial state', () => {
    expect(component.shopId).toBe(0);
    expect(component.supplierId).toBe(0);
    expect(component.amount).toBe(0);
    expect(component.currency).toBe('TND');
    expect(component.creating).toBeFalse();
  });

  describe('ngOnInit', () => {
    it('should load shops and suppliers for SYSTEM_ADMIN', () => {
      sessionStorage.setItem('user', JSON.stringify({ roles: ['SYSTEM_ADMIN'], organizationId: 1 }));
      component.ngOnInit();
      expect(orgService.listShops).toHaveBeenCalled();
      expect(orgService.listSuppliers).toHaveBeenCalled();
    });

    it('should set shopId for SHOP_ADMIN role', () => {
      sessionStorage.setItem('user', JSON.stringify({ roles: ['SHOP_ADMIN'], organizationId: 5, username: 'shop1' }));
      component.ngOnInit();
      expect(component.shopId).toBe(5);
      expect(component.shops.length).toBe(1);
    });

    it('should set shopId for SHOP_MANAGER role', () => {
      sessionStorage.setItem('user', JSON.stringify({ roles: ['SHOP_MANAGER'], organizationId: 3, username: 'mgr' }));
      component.ngOnInit();
      expect(component.shopId).toBe(3);
    });

    it('should load suppliers for shop with active relations', () => {
      sessionStorage.setItem('user', JSON.stringify({ roles: ['SHOP_ADMIN'], organizationId: 5, username: 'shop1' }));
      orgService.listRelationsByShop.and.returnValue(of([{ supplierId: 10, status: 'ACTIVE' } as any, { supplierId: 20, status: 'INACTIVE' } as any]));
      component.ngOnInit();
      expect(orgService.listRelationsByShop).toHaveBeenCalledWith(5);
      expect(orgService.getById).toHaveBeenCalledWith(10);
    });

    it('should do nothing when no user in session', () => {
      component.ngOnInit();
      expect(orgService.listShops).not.toHaveBeenCalled();
    });

    it('should do nothing when user has no roles', () => {
      sessionStorage.setItem('user', JSON.stringify({ roles: [], organizationId: 1 }));
      component.ngOnInit();
      expect(orgService.listShops).not.toHaveBeenCalled();
    });

    it('should not load shopId when organizationId is missing', () => {
      sessionStorage.setItem('user', JSON.stringify({ roles: ['SHOP_ADMIN'], username: 'shop1' }));
      component.ngOnInit();
      expect(component.shopId).toBe(0);
    });
  });

  describe('create', () => {
    it('should create payment and navigate', () => {
      component.shopId = 1;
      component.supplierId = 2;
      component.amount = 100;
      component.create();
      expect(paymentService.create).toHaveBeenCalledWith({ shopId: 1, supplierId: 2, amount: 100, currency: 'TND' });
      expect(router.navigate).toHaveBeenCalledWith(['/dashboard/payments', 1]);
    });

    it('should not create when shopId is 0', () => {
      component.supplierId = 2;
      component.amount = 100;
      component.create();
      expect(paymentService.create).not.toHaveBeenCalled();
    });

    it('should not create when supplierId is 0', () => {
      component.shopId = 1;
      component.amount = 100;
      component.create();
      expect(paymentService.create).not.toHaveBeenCalled();
    });

    it('should not create when amount is 0', () => {
      component.shopId = 1;
      component.supplierId = 2;
      component.amount = 0;
      component.create();
      expect(paymentService.create).not.toHaveBeenCalled();
    });

    it('should not create when amount is negative', () => {
      component.shopId = 1;
      component.supplierId = 2;
      component.amount = -10;
      component.create();
      expect(paymentService.create).not.toHaveBeenCalled();
    });

    it('should handle create error', () => {
      component.shopId = 1;
      component.supplierId = 2;
      component.amount = 100;
      paymentService.create.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
      component.create();
      expect(toast.error).toHaveBeenCalledWith('Err');
      expect(component.creating).toBeFalse();
    });

    it('should handle create error without message', () => {
      component.shopId = 1;
      component.supplierId = 2;
      component.amount = 100;
      paymentService.create.and.returnValue(throwError(() => ({})));
      component.create();
      expect(toast.error).toHaveBeenCalledWith('Erreur lors de la création');
    });
  });
});
