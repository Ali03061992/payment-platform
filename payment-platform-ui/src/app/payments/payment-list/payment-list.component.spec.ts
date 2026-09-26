// @ts-nocheck
/**
 * Tests du composant PaymentListComponent.
 * Perimetre : cas should load payments on init; should handle load error; should filter payments by status (voir blocs describe/it).
 * Moyens : TestBed + fixture, stubs jasmine, client HTTP de test, observables RxJS mockes.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';
import { PaymentListComponent } from './payment-list.component';
import { PaymentService } from '../../services/payment.service';
import { ToastService } from '../../services/toast.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('PaymentListComponent', () => {
  let component: PaymentListComponent;
  let fixture: ComponentFixture<PaymentListComponent>;
  let paymentService: jasmine.SpyObj<PaymentService>;
  let toast: jasmine.SpyObj<ToastService>;

  const mockPayments = [
    { id: 1, status: 'PENDING', amount: 100, reference: 'REF1' } as any,
    { id: 2, status: 'CONFIRMED', amount: 200, reference: 'REF2' } as any,
    { id: 3, status: 'CANCELLED', amount: 300, reference: 'REF3' } as any,
  ];

  beforeEach(() => {
    const psSpy = jasmine.createSpyObj('PaymentService', ['list', 'confirm', 'cancel']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    psSpy.list.and.returnValue(of(mockPayments));
    psSpy.confirm.and.returnValue(of({} as any));
    psSpy.cancel.and.returnValue(of({} as any));

    TestBed.configureTestingModule({
    declarations: [PaymentListComponent],
    schemas: [NO_ERRORS_SCHEMA],
    imports: [],
    providers: [
        { provide: PaymentService, useValue: psSpy },
        { provide: ToastService, useValue: toastSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(PaymentListComponent);
    component = fixture.componentInstance;
    paymentService = TestBed.inject(PaymentService) as jasmine.SpyObj<PaymentService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load payments on init', () => {
    component.ngOnInit();
    expect(component.payments.length).toBe(3);
    expect(component.loading).toBeFalse();
  });

  it('should handle load error', () => {
    paymentService.list.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
    component.load();
    expect(toast.error).toHaveBeenCalledWith('Err');
    expect(component.loading).toBeFalse();
  });

  it('should filter payments by status', () => {
    component.payments = mockPayments;
    expect(component.filteredPayments.length).toBe(3);
    component.filterStatus = 'PENDING';
    expect(component.filteredPayments.length).toBe(1);
    expect(component.filteredPayments[0].id).toBe(1);
  });

  it('should return all payments when filter is empty', () => {
    component.payments = mockPayments;
    component.filterStatus = '';
    expect(component.filteredPayments.length).toBe(3);
  });

  it('should return correct status label', () => {
    expect(component.statusLabel('PENDING')).toBe('En attente');
    expect(component.statusLabel('CONFIRMED')).toBe('Confirmé');
    expect(component.statusLabel('REJECTED')).toBe('Rejeté');
    expect(component.statusLabel('CANCELLED')).toBe('Annulé');
    expect(component.statusLabel('UNKNOWN')).toBe('UNKNOWN');
  });

  it('should return correct status class', () => {
    expect(component.statusClass('PENDING')).toBe('pending');
    expect(component.statusClass('CONFIRMED')).toBe('confirmed');
    expect(component.statusClass('REJECTED')).toBe('rejected');
    expect(component.statusClass('CANCELLED')).toBe('cancelled');
    expect(component.statusClass('UNKNOWN')).toBe('');
  });

  it('should confirm payment', () => {
    component.confirm(1);
    expect(paymentService.confirm).toHaveBeenCalledWith(1);
  });

  it('should cancel payment', () => {
    component.cancel(1);
    expect(paymentService.cancel).toHaveBeenCalledWith(1);
  });

  it('should handle confirm error', () => {
    paymentService.confirm.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
    component.confirm(1);
    expect(toast.error).toHaveBeenCalledWith('Err');
  });

  it('should handle cancel error', () => {
    paymentService.cancel.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
    component.cancel(1);
    expect(toast.error).toHaveBeenCalledWith('Err');
  });

  it('should unsubscribe on destroy', () => {
    component.ngOnInit();
    expect(() => component.ngOnDestroy()).not.toThrow();
  });
});
