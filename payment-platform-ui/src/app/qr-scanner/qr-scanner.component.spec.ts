// @ts-nocheck
/**
 * Tests du composant QrScannerComponent.
 * Perimetre : instanciation et blocs ngOnInit, ngOnDestroy, isCameraSupported, startCamera, extractPaymentId (voir blocs describe/it).
 * Moyens : TestBed + fixture, stubs jasmine.
 */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import { QrScannerComponent } from './qr-scanner.component';
import { ToastService } from '../services/toast.service';

describe('QrScannerComponent', () => {
  let component: QrScannerComponent;
  let fixture: ComponentFixture<QrScannerComponent>;
  let toast: jasmine.SpyObj<ToastService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    const toastSpy = jasmine.createSpyObj('ToastService', ['error', 'success']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    TestBed.configureTestingModule({
      declarations: [QrScannerComponent],
      providers: [
        { provide: ToastService, useValue: toastSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });
    fixture = TestBed.createComponent(QrScannerComponent);
    component = fixture.componentInstance;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have initial state', () => {
    expect(component.scanning).toBeFalse();
    expect(component.manualUrl).toBe('');
    expect(component.useManual).toBeTrue();
    expect(component.cameraActive).toBeFalse();
    expect(component.cameraError).toBe('');
  });

  describe('ngOnInit', () => {
    it('should set isSecureContext from window', () => {
      component.ngOnInit();
      expect(typeof component.isSecureContext).toBe('boolean');
    });
  });

  describe('ngOnDestroy', () => {
    it('should stop camera on destroy', () => {
      component.cameraActive = true;
      component.ngOnDestroy();
      expect(component.cameraActive).toBeFalse();
      expect(component.scanning).toBeFalse();
    });
  });

  describe('isCameraSupported', () => {
    it('should return boolean', () => {
      const result = component.isCameraSupported();
      expect(typeof result).toBe('boolean');
    });
  });

  describe('startCamera', () => {
    it('should set error when not secure context', async () => {
      component.isSecureContext = false;
      await component.startCamera();
      expect(component.cameraError).toContain('HTTPS');
      expect(component.useManual).toBeTrue();
    });

    it('should reset useManual and cameraError before checks', async () => {
      component.isSecureContext = false;
      component.useManual = false;
      component.cameraError = 'previous error';
      await component.startCamera();
      expect(component.cameraError).toContain('HTTPS');
      expect(component.useManual).toBeTrue();
    });
  });

  describe('extractPaymentId', () => {
    it('should extract from dashboard payments URL', () => {
      expect(component.extractPaymentId('/dashboard/payments/123')).toBe(123);
    });

    it('should extract from payments URL', () => {
      expect(component.extractPaymentId('/payments/456')).toBe(456);
    });

    it('should extract from payment_id parameter', () => {
      expect(component.extractPaymentId('payment_id=789')).toBe(789);
    });

    it('should extract from payment-id parameter', () => {
      expect(component.extractPaymentId('payment-id=789')).toBe(789);
    });

    it('should extract from facture parameter', () => {
      expect(component.extractPaymentId('facture=101')).toBe(101);
    });

    it('should extract plain number', () => {
      expect(component.extractPaymentId('42')).toBe(42);
    });

    it('should extract plain number with spaces', () => {
      expect(component.extractPaymentId('  42  ')).toBe(42);
    });

    it('should return null for invalid data', () => {
      expect(component.extractPaymentId('not-a-number')).toBeNull();
    });

    it('should return null for empty string', () => {
      expect(component.extractPaymentId('')).toBeNull();
    });

    it('should return null for mixed text', () => {
      expect(component.extractPaymentId('hello world')).toBeNull();
    });

    it('should return null for text with no digits', () => {
      expect(component.extractPaymentId('abc')).toBeNull();
    });
  });

  describe('submitManual', () => {
    it('should navigate for valid payment id in dashboard URL', () => {
      component.manualUrl = '/dashboard/payments/123';
      component.submitManual();
      expect(router.navigate).toHaveBeenCalledWith(['/dashboard/payments', 123]);
    });

    it('should navigate for plain number', () => {
      component.manualUrl = '456';
      component.submitManual();
      expect(router.navigate).toHaveBeenCalledWith(['/dashboard/payments', 456]);
    });

    it('should navigate for payment_id param', () => {
      component.manualUrl = 'payment_id=789';
      component.submitManual();
      expect(router.navigate).toHaveBeenCalledWith(['/dashboard/payments', 789]);
    });

    it('should show error for invalid input', () => {
      component.manualUrl = 'invalid';
      component.submitManual();
      expect(toast.error).toHaveBeenCalledWith('Lien ou ID non reconnu. Essayez un numéro de facture.');
    });

    it('should do nothing for empty input', () => {
      component.manualUrl = '';
      component.submitManual();
      expect(toast.error).not.toHaveBeenCalled();
      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('should do nothing for whitespace-only input', () => {
      component.manualUrl = '   ';
      component.submitManual();
      expect(toast.error).not.toHaveBeenCalled();
      expect(router.navigate).not.toHaveBeenCalled();
    });
  });

  describe('stopCamera', () => {
    it('should stop camera when codeReader is set', () => {
      component.cameraActive = true;
      component.scanning = true;
      component['codeReader'] = { reset: jasmine.createSpy('reset') } as any;
      component.stopCamera();
      expect(component.cameraActive).toBeFalse();
      expect(component.scanning).toBeFalse();
      expect(component['codeReader']).toBeNull();
    });

    it('should handle null codeReader', () => {
      component.stopCamera();
      expect(component.cameraActive).toBeFalse();
      expect(component.scanning).toBeFalse();
    });
  });

  describe('goBack', () => {
    it('should stop camera and navigate to dashboard', () => {
      component.goBack();
      expect(component.cameraActive).toBeFalse();
      expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    });
  });

  describe('handleResult', () => {
    it('should navigate when valid payment id in data', () => {
      component.handleResult('/dashboard/payments/42');
      expect(router.navigate).toHaveBeenCalledWith(['/dashboard/payments', 42]);
    });

    it('should show error when data is not valid', () => {
      component.handleResult('not-valid-data');
      expect(toast.error).toHaveBeenCalledWith('QR Code non reconnu comme facture valide.');
    });

    it('should stop camera when called', () => {
      component.cameraActive = true;
      component.scanning = true;
      component.handleResult('/dashboard/payments/1');
      expect(component.cameraActive).toBeFalse();
      expect(component.scanning).toBeFalse();
    });

    it('should do nothing for empty data', () => {
      component.handleResult('');
      expect(router.navigate).not.toHaveBeenCalled();
      expect(toast.error).not.toHaveBeenCalled();
    });

    it('should navigate for plain number data', () => {
      component.handleResult('99');
      expect(router.navigate).toHaveBeenCalledWith(['/dashboard/payments', 99]);
    });

    it('should navigate for facture param data', () => {
      component.handleResult('facture=55');
      expect(router.navigate).toHaveBeenCalledWith(['/dashboard/payments', 55]);
    });

    it('should navigate for payment_id param data', () => {
      component.handleResult('payment-id=33');
      expect(router.navigate).toHaveBeenCalledWith(['/dashboard/payments', 33]);
    });
  });
});
