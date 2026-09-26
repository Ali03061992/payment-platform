// @ts-nocheck
/**
 * Tests du composant SupplierBalanceComponent.
 * Perimetre : cas should load balances on init; should not credit when invalid; should credit valid shop (voir blocs describe/it).
 * Moyens : TestBed + fixture, stubs jasmine, client HTTP de test, observables RxJS mockes.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { SupplierBalanceComponent } from './supplier-balance.component';
import { BalanceService } from '../../services/balance.service';
import { LoginService } from '../../services/login.service';
import { ToastService } from '../../services/toast.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('SupplierBalanceComponent', () => {
  let component: SupplierBalanceComponent;
  let fixture: ComponentFixture<SupplierBalanceComponent>;
  let balanceService: jasmine.SpyObj<BalanceService>;
  let toast: jasmine.SpyObj<ToastService>;

  beforeEach(() => {
    const balanceSpy = jasmine.createSpyObj('BalanceService', ['listBySupplier', 'getHistory', 'adjust']);
    const loginSpy = { getCurrentUser: () => ({ organizationId: 'sup-1' }) };
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    balanceSpy.listBySupplier.and.returnValue(of([]));
    balanceSpy.getHistory.and.returnValue(of([]));

    TestBed.configureTestingModule({
      declarations: [SupplierBalanceComponent],
      schemas: [NO_ERRORS_SCHEMA],
      imports: [FormsModule],
      providers: [
        { provide: BalanceService, useValue: balanceSpy },
        { provide: LoginService, useValue: loginSpy },
        { provide: ToastService, useValue: toastSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    fixture = TestBed.createComponent(SupplierBalanceComponent);
    component = fixture.componentInstance;
    balanceService = TestBed.inject(BalanceService) as jasmine.SpyObj<BalanceService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load balances on init', () => {
    component.ngOnInit();
    expect(balanceService.listBySupplier).toHaveBeenCalledWith('sup-1');
  });

  it('should not credit when invalid', () => {
    component.creditShopId = '';
    component.creditAmount = null;
    expect(component.canCredit()).toBeFalse();
  });

  it('should credit valid shop', () => {
    balanceService.adjust.and.returnValue(of({} as any));
    component.supplierId = 'sup-1';
    component.creditShopId = 'shop-1';
    component.creditAmount = 100;
    component.creditReason = 'test';
    expect(component.canCredit()).toBeTrue();
    component.submitCredit();
    expect(balanceService.adjust).toHaveBeenCalled();
    expect(toast.success).toHaveBeenCalled();
  });

  it('should handle credit error', () => {
    balanceService.adjust.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
    component.supplierId = 'sup-1';
    component.creditShopId = 'shop-1';
    component.creditAmount = 100;
    component.submitCredit();
    expect(toast.error).toHaveBeenCalledWith('Err');
  });
});
