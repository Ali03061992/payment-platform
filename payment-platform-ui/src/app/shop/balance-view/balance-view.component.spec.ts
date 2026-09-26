// @ts-nocheck
/**
 * Tests du composant BalanceViewComponent.
 * Perimetre : instanciation et blocs ngOnInit, getShopId (private), load, viewLedger, closeLedger (voir blocs describe/it).
 * Moyens : TestBed + fixture, stubs jasmine, client HTTP de test, observables RxJS mockes.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { BalanceViewComponent } from './balance-view.component';
import { BalanceService } from '../../services/balance.service';
import { ToastService } from '../../services/toast.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('BalanceViewComponent', () => {
  let component: BalanceViewComponent;
  let fixture: ComponentFixture<BalanceViewComponent>;
  let balanceService: jasmine.SpyObj<BalanceService>;
  let toast: jasmine.SpyObj<ToastService>;

  beforeEach(() => {
    sessionStorage.setItem('user', JSON.stringify({ organizationId: 2 }));
    const balanceSpy = jasmine.createSpyObj('BalanceService', ['listByShop', 'getHistory']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    balanceSpy.listByShop.and.returnValue(of([]));

    TestBed.configureTestingModule({
    declarations: [BalanceViewComponent],
    schemas: [NO_ERRORS_SCHEMA],
    imports: [],
    providers: [
        { provide: BalanceService, useValue: balanceSpy },
        { provide: ToastService, useValue: toastSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(BalanceViewComponent);
    component = fixture.componentInstance;
    balanceService = TestBed.inject(BalanceService) as jasmine.SpyObj<BalanceService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  afterEach(() => sessionStorage.clear());

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should call load', () => {
      component.ngOnInit();
      expect(balanceService.listByShop).toHaveBeenCalled();
    });
  });

  describe('getShopId (private)', () => {
    it('should return organizationId from session', () => {
      expect(component['getShopId']()).toBe(2);
    });

    it('should return empty string when no user', () => {
      sessionStorage.clear();
      expect(component['getShopId']()).toBe('');
    });

    it('should return empty string when user has no organizationId', () => {
      sessionStorage.setItem('user', JSON.stringify({ username: 'test' }));
      expect(component['getShopId']()).toBe('');
    });
  });

  describe('load', () => {
    it('should load balances', () => {
      const mock = [
        { supplierId: 1, shopId: 2, supplierName: 'Sup', shopName: 'Shop', currentBalance: 500, totalOrders: 10, totalPayments: 5, remainingDue: 250, lastTransaction: '' }
      ];
      balanceService.listByShop.and.returnValue(of(mock));
      component.load();
      expect(component.balances.length).toBe(1);
      expect(component.loading).toBeFalse();
    });

    it('should handle error with message', () => {
      balanceService.listByShop.and.returnValue(throwError(() => ({ error: { message: 'Fail' } })));
      component.load();
      expect(toast.error).toHaveBeenCalledWith('Fail');
      expect(component.loading).toBeFalse();
    });

    it('should handle error without message', () => {
      balanceService.listByShop.and.returnValue(throwError(() => ({})));
      component.load();
      expect(toast.error).toHaveBeenCalledWith('Erreur');
    });
  });

  describe('viewLedger', () => {
    it('should load ledger', () => {
      balanceService.getHistory.and.returnValue(of([{ id: 1 } as any]));
      component.viewLedger(1, 2);
      expect(component.selectedSupplierId).toBe(1);
      expect(component.selectedShopId).toBe(2);
      expect(component.loadingLedger).toBeFalse();
      expect(component.ledgerEntries.length).toBe(1);
    });

    it('should handle ledger load error', () => {
      balanceService.getHistory.and.returnValue(throwError(() => ({ error: { message: 'Fail' } })));
      component.viewLedger(1, 2);
      expect(component.loadingLedger).toBeFalse();
      expect(toast.error).toHaveBeenCalledWith('Fail');
    });

    it('should handle ledger load error without message', () => {
      balanceService.getHistory.and.returnValue(throwError(() => ({})));
      component.viewLedger(1, 2);
      expect(toast.error).toHaveBeenCalledWith('Erreur');
    });
  });

  describe('closeLedger', () => {
    it('should close ledger', () => {
      component.closeLedger();
      expect(component.selectedSupplierId).toBeNull();
      expect(component.selectedShopId).toBeNull();
      expect(component.ledgerEntries.length).toBe(0);
    });
  });

  describe('typeLabel', () => {
    it('should return correct labels', () => {
      expect(component.typeLabel('ORDER')).toBe('Commande');
      expect(component.typeLabel('PAYMENT')).toBe('Paiement');
      expect(component.typeLabel('ADJUSTMENT')).toBe('Ajustement');
      expect(component.typeLabel('UNKNOWN')).toBe('UNKNOWN');
    });
  });

  describe('typeClass', () => {
    it('should return correct classes', () => {
      expect(component.typeClass('ORDER')).toBe('order');
      expect(component.typeClass('PAYMENT')).toBe('payment');
      expect(component.typeClass('ADJUSTMENT')).toBe('adjustment');
      expect(component.typeClass('UNKNOWN')).toBe('');
    });
  });

  describe('getTotalRemaining', () => {
    it('should sum remainingDue', () => {
      component.balances = [
        { remainingDue: 100 } as any,
        { remainingDue: 200 } as any
      ];
      expect(component.getTotalRemaining()).toBe(300);
    });

    it('should return 0 for empty balances', () => {
      component.balances = [];
      expect(component.getTotalRemaining()).toBe(0);
    });
  });
});
