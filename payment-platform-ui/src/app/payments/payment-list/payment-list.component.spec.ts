import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { PaymentListComponent } from './payment-list.component';

describe('PaymentListComponent', () => {
  let component: PaymentListComponent;
  let fixture: ComponentFixture<PaymentListComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [PaymentListComponent]
    });
    fixture = TestBed.createComponent(PaymentListComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have initial state', () => {
    expect(component.payments).toEqual([]);
    expect(component.loading).toBeTrue();
    expect(component.filterStatus).toBe('');
  });

  describe('filteredPayments', () => {
    it('should return all payments when filter is empty', () => {
      component.payments = [
        { id: 1, reference: 'PAY-001', shopId: 1, supplierId: 2, amount: 100, currency: 'TND', status: 'PENDING', rejectionReason: '', createdBy: 1, version: 1, createdAt: '', updatedAt: '', events: [] },
        { id: 2, reference: 'PAY-002', shopId: 1, supplierId: 2, amount: 200, currency: 'TND', status: 'CONFIRMED', rejectionReason: '', createdBy: 1, version: 1, createdAt: '', updatedAt: '', events: [] }
      ];
      component.filterStatus = '';
      expect(component.filteredPayments.length).toBe(2);
    });

    it('should filter by status', () => {
      component.payments = [
        { id: 1, reference: 'PAY-001', shopId: 1, supplierId: 2, amount: 100, currency: 'TND', status: 'PENDING', rejectionReason: '', createdBy: 1, version: 1, createdAt: '', updatedAt: '', events: [] },
        { id: 2, reference: 'PAY-002', shopId: 1, supplierId: 2, amount: 200, currency: 'TND', status: 'CONFIRMED', rejectionReason: '', createdBy: 1, version: 1, createdAt: '', updatedAt: '', events: [] }
      ];
      component.filterStatus = 'PENDING';
      expect(component.filteredPayments.length).toBe(1);
      expect(component.filteredPayments[0].status).toBe('PENDING');
    });
  });

  describe('statusLabel', () => {
    it('should return French labels', () => {
      expect(component.statusLabel('PENDING')).toBe('En attente');
      expect(component.statusLabel('CONFIRMED')).toBe('Confirmé');
      expect(component.statusLabel('REJECTED')).toBe('Rejeté');
      expect(component.statusLabel('CANCELLED')).toBe('Annulé');
    });

    it('should return raw value for unknown status', () => {
      expect(component.statusLabel('UNKNOWN')).toBe('UNKNOWN');
    });
  });

  describe('statusClass', () => {
    it('should return correct CSS classes', () => {
      expect(component.statusClass('PENDING')).toBe('pending');
      expect(component.statusClass('CONFIRMED')).toBe('confirmed');
      expect(component.statusClass('REJECTED')).toBe('rejected');
      expect(component.statusClass('CANCELLED')).toBe('cancelled');
    });
  });
});
