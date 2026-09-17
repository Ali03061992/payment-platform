// @ts-nocheck
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { PaymentDetailComponent } from './payment-detail.component';
import { PaymentService } from '../../services/payment.service';
import { ToastService } from '../../services/toast.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('PaymentDetailComponent', () => {
  let component: PaymentDetailComponent;
  let fixture: ComponentFixture<PaymentDetailComponent>;
  let router: jasmine.SpyObj<Router>;
  let paymentService: jasmine.SpyObj<PaymentService>;
  let toast: jasmine.SpyObj<ToastService>;

  const mockPayment = { id: 1, status: 'PENDING', amount: 100, currency: 'TND', reference: 'REF1' } as any;

  beforeEach(() => {
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const psSpy = jasmine.createSpyObj('PaymentService', ['getById', 'confirm', 'reject', 'cancel']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    psSpy.getById.and.returnValue(of(mockPayment));
    psSpy.confirm.and.returnValue(of({ ...mockPayment, status: 'CONFIRMED' }));
    psSpy.reject.and.returnValue(of({ ...mockPayment, status: 'REJECTED' }));
    psSpy.cancel.and.returnValue(of({ ...mockPayment, status: 'CANCELLED' }));

    TestBed.configureTestingModule({
    declarations: [PaymentDetailComponent],
    schemas: [NO_ERRORS_SCHEMA],
    imports: [],
    providers: [
        { provide: Router, useValue: routerSpy },
        { provide: PaymentService, useValue: psSpy },
        { provide: ToastService, useValue: toastSpy },
        {
            provide: ActivatedRoute,
            useValue: { snapshot: { paramMap: convertToParamMap({ id: '1' }) } }
        },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(PaymentDetailComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    paymentService = TestBed.inject(PaymentService) as jasmine.SpyObj<PaymentService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have initial state', () => {
    expect(component.payment).toBeNull();
    expect(component.loading).toBeTrue();
    expect(component.showReject).toBeFalse();
    expect(component.rejectReason).toBe('');
  });

  describe('ngOnInit', () => {
    it('should load payment by id', () => {
      component.ngOnInit();
      expect(paymentService.getById).toHaveBeenCalledWith(1);
      expect(component.payment).toBeTruthy();
      expect(component.loading).toBeFalse();
    });

    it('should set qrData', () => {
      component.ngOnInit();
      expect(component.qrData).toContain('/dashboard/payments/1');
    });

    it('should handle load error and navigate', () => {
      paymentService.getById.and.returnValue(throwError(() => new Error('fail')));
      component.ngOnInit();
      expect(component.loading).toBeFalse();
      expect(router.navigate).toHaveBeenCalledWith(['/dashboard/payments']);
    });
  });

  describe('ngOnDestroy', () => {
    it('should unsubscribe', () => {
      component.ngOnInit();
      expect(() => component.ngOnDestroy()).not.toThrow();
    });
  });

  describe('confirm', () => {
    it('should confirm payment', () => {
      component.ngOnInit();
      component.confirm();
      expect(paymentService.confirm).toHaveBeenCalledWith(1);
      expect(toast.success).toHaveBeenCalledWith('Paiement confirmé');
      expect(component.payment?.status).toBe('CONFIRMED');
    });

    it('should not confirm without payment', () => {
      component.payment = null;
      component.confirm();
      expect(paymentService.confirm).not.toHaveBeenCalled();
    });

    it('should handle confirm error', () => {
      paymentService.confirm.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
      component.ngOnInit();
      component.confirm();
      expect(toast.error).toHaveBeenCalledWith('Err');
    });

    it('should handle confirm error without message', () => {
      paymentService.confirm.and.returnValue(throwError(() => ({})));
      component.ngOnInit();
      component.confirm();
      expect(toast.error).toHaveBeenCalledWith('Erreur');
    });
  });

  describe('reject', () => {
    it('should reject payment', () => {
      component.ngOnInit();
      component.rejectReason = 'bad';
      component.reject();
      expect(paymentService.reject).toHaveBeenCalledWith(1, { rejectionReason: 'bad' });
      expect(toast.success).toHaveBeenCalledWith('Paiement rejeté');
      expect(component.showReject).toBeFalse();
      expect(component.rejectReason).toBe('');
    });

    it('should not reject without payment', () => {
      component.payment = null;
      component.rejectReason = 'reason';
      component.reject();
      expect(paymentService.reject).not.toHaveBeenCalled();
    });

    it('should not reject with empty reason', () => {
      component.ngOnInit();
      component.rejectReason = '';
      component.reject();
      expect(paymentService.reject).not.toHaveBeenCalled();
    });

    it('should not reject with whitespace-only reason', () => {
      component.ngOnInit();
      component.rejectReason = '   ';
      component.reject();
      expect(paymentService.reject).not.toHaveBeenCalled();
    });

    it('should handle reject error', () => {
      paymentService.reject.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
      component.ngOnInit();
      component.rejectReason = 'reason';
      component.reject();
      expect(toast.error).toHaveBeenCalledWith('Err');
    });

    it('should handle reject error without message', () => {
      paymentService.reject.and.returnValue(throwError(() => ({})));
      component.ngOnInit();
      component.rejectReason = 'reason';
      component.reject();
      expect(toast.error).toHaveBeenCalledWith('Erreur');
    });
  });

  describe('cancel', () => {
    it('should cancel payment', () => {
      component.ngOnInit();
      component.cancel();
      expect(paymentService.cancel).toHaveBeenCalledWith(1);
      expect(toast.success).toHaveBeenCalledWith('Paiement annulé');
    });

    it('should not cancel without payment', () => {
      component.payment = null;
      component.cancel();
      expect(paymentService.cancel).not.toHaveBeenCalled();
    });

    it('should handle cancel error', () => {
      paymentService.cancel.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
      component.ngOnInit();
      component.cancel();
      expect(toast.error).toHaveBeenCalledWith('Err');
    });

    it('should handle cancel error without message', () => {
      paymentService.cancel.and.returnValue(throwError(() => ({})));
      component.ngOnInit();
      component.cancel();
      expect(toast.error).toHaveBeenCalledWith('Erreur');
    });
  });

  describe('toggleQR', () => {
    it('should toggle showQR', () => {
      expect(component.showQR).toBeFalse();
      component.toggleQR();
      expect(component.showQR).toBeTrue();
      component.toggleQR();
      expect(component.showQR).toBeFalse();
    });
  });

  describe('shareQR', () => {
    it('should not crash when navigator.share is not available', () => {
      component.ngOnInit();
      component.shareQR();
    });
  });

  describe('statusLabel', () => {
    it('should return French labels', () => {
      expect(component.statusLabel('PENDING')).toBe('En attente');
      expect(component.statusLabel('CONFIRMED')).toBe('Confirmé');
      expect(component.statusLabel('REJECTED')).toBe('Rejeté');
      expect(component.statusLabel('CANCELLED')).toBe('Annulé');
      expect(component.statusLabel('UNKNOWN')).toBe('UNKNOWN');
    });
  });

  describe('actionLabel', () => {
    it('should return French labels', () => {
      expect(component.actionLabel('PAYMENT_CREATED')).toBe('Créé');
      expect(component.actionLabel('PAYMENT_CONFIRMED')).toBe('Confirmé');
      expect(component.actionLabel('PAYMENT_REJECTED')).toBe('Rejeté');
      expect(component.actionLabel('PAYMENT_CANCELLED')).toBe('Annulé');
      expect(component.actionLabel('UNKNOWN')).toBe('UNKNOWN');
    });
  });
});
