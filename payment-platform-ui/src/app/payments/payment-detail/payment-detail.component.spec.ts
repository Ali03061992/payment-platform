import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { PaymentDetailComponent } from './payment-detail.component';

describe('PaymentDetailComponent', () => {
  let component: PaymentDetailComponent;
  let fixture: ComponentFixture<PaymentDetailComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule],
      declarations: [PaymentDetailComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => '1' } } }
        }
      ]
    });
    fixture = TestBed.createComponent(PaymentDetailComponent);
    component = fixture.componentInstance;
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

  describe('statusLabel', () => {
    it('should return French labels', () => {
      expect(component.statusLabel('PENDING')).toBe('En attente');
      expect(component.statusLabel('CONFIRMED')).toBe('Confirmé');
    });
  });

  describe('actionLabel', () => {
    it('should return French labels', () => {
      expect(component.actionLabel('PAYMENT_CREATED')).toBe('Créé');
      expect(component.actionLabel('PAYMENT_CONFIRMED')).toBe('Confirmé');
      expect(component.actionLabel('PAYMENT_REJECTED')).toBe('Rejeté');
      expect(component.actionLabel('PAYMENT_CANCELLED')).toBe('Annulé');
    });

    it('should return raw value for unknown action', () => {
      expect(component.actionLabel('UNKNOWN')).toBe('UNKNOWN');
    });
  });
});
