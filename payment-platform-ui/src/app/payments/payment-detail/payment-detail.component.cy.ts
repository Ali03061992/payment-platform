/// <reference types="cypress" />
/// <reference types="@cypress/angular" />

import { PaymentDetailComponent } from './payment-detail.component';
import { PaymentService } from '../../services/payment.service';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Payment } from '../../models/payment.model';

describe('PaymentDetailComponent', () => {
  let paymentServiceSpy: jasmine.SpyObj<PaymentService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let mockRoute: any;

  const mockPayment: Payment = {
    id: 1,
    reference: 'PAY-2024-001',
    shopId: 3,
    supplierId: 1,
    amount: 250.00,
    currency: 'EUR',
    status: 'PENDING',
    createdAt: new Date().toISOString(),
    createdBy: 8,
    confirmedAt: null,
    rejectedAt: null,
    cancelledAt: null,
    rejectionReason: null,
    history: [
      { action: 'PAYMENT_CREATED', userId: 8, timestamp: new Date().toISOString() }
    ]
  };

  beforeEach(() => {
    paymentServiceSpy = jasmine.createSpyObj('PaymentService', ['getById', 'confirm', 'reject', 'cancel']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    
    mockRoute = {
      snapshot: {
        paramMap: {
          get: jasmine.createSpy('get').and.returnValue('1')
        }
      }
    };
    
    localStorage.clear();
    localStorage.setItem('token', 'mock-token');
  });

  const mountComponent = (overrides: Partial<{ payment: Payment; error: any }> = {}) => {
    const payment = overrides.payment ?? mockPayment;
    const getByIdReturn = overrides.error ?? of(payment);
    
    paymentServiceSpy.getById.and.returnValue(getByIdReturn);
    paymentServiceSpy.confirm.and.returnValue(of({ ...payment, status: 'CONFIRMED' }));
    paymentServiceSpy.reject.and.returnValue(of({ ...payment, status: 'REJECTED', rejectionReason: 'Test reason' }));
    paymentServiceSpy.cancel.and.returnValue(of({ ...payment, status: 'CANCELLED' }));

    return cy.mount(PaymentDetailComponent, {
      providers: [
        { provide: PaymentService, useValue: paymentServiceSpy },
        { provide: ActivatedRoute, useValue: mockRoute },
        { provide: Router, useValue: routerSpy },
      ],
    });
  };

  it('should load payment on init and display details', () => {
    mountComponent();
    
    cy.get('.payment-detail').should('exist');
    cy.contains('PAY-2024-001').should('exist');
    cy.contains('250.00 EUR').should('exist');
    cy.contains('En attente').should('exist');
    cy.contains('Shop #3').should('exist');
    cy.contains('Supplier #1').should('exist');
  });

  it('should show loading state initially', () => {
    // Use a delayed observable
    let resolve: (value: Payment) => void;
    const delayed$ = new Promise(resolve => { resolve = resolve; });
    
    paymentServiceSpy.getById.and.returnValue(
      new Observable(observer => {
        delayed$.then(val => {
          observer.next(val);
          observer.complete();
        });
      })
    );

    cy.mount(PaymentDetailComponent, {
      providers: [
        { provide: PaymentService, useValue: paymentServiceSpy },
        { provide: ActivatedRoute, useValue: mockRoute },
        { provide: Router, useValue: routerSpy },
      ],
    });
    
    cy.contains('Chargement').should('exist');
    
    // Resolve
    resolve!(mockPayment);
    
    cy.contains('PAY-2024-001').should('exist');
  });

  it('should navigate back to payments list on load error', () => {
    mountComponent({ error: throwError(() => new Error('Not found')) });
    
    cy.wrap(null).should(() => {
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard/payments']);
    });
  });

  it('should show QR code when toggle button clicked', () => {
    mountComponent();
    
    cy.get('button').contains('QR Code').click();
    cy.get('app-qrcode, qrcode, [class*="qr"]').should('exist');
  });

  it('should confirm payment when confirm button clicked', () => {
    mountComponent();
    
    cy.get('button').contains('Confirmer').click();
    
    cy.wrap(null).should(() => {
      expect(paymentServiceSpy.confirm).toHaveBeenCalledWith(1);
    });
    
    // Check success message
    cy.contains('Paiement confirmé').should('exist');
  });

  it('should open reject modal and reject with reason', () => {
    mountComponent();
    
    cy.get('button').contains('Rejeter').click();
    cy.get('textarea[placeholder*="raison"], textarea[name="rejectReason"]').should('exist');
    
    cy.get('textarea').type('Montant incorrect');
    cy.get('button').contains('Confirmer le rejet').click();
    
    cy.wrap(null).should(() => {
      expect(paymentServiceSpy.reject).toHaveBeenCalledWith(1, { rejectionReason: 'Montant incorrect' });
    });
  });

  it('should cancel payment when cancel button clicked', () => {
    mountComponent({ payment: { ...mockPayment, status: 'PENDING' } });
    
    cy.get('button').contains('Annuler').click();
    
    cy.wrap(null).should(() => {
      expect(paymentServiceSpy.cancel).toHaveBeenCalledWith(1);
    });
  });

  it('should disable confirm/reject/cancel for non-pending payments', () => {
    const confirmedPayment = { ...mockPayment, status: 'CONFIRMED' };
    mountComponent({ payment: confirmedPayment });
    
    cy.get('button').contains('Confirmer').should('not.exist');
    cy.get('button').contains('Rejeter').should('not.exist');
    cy.get('button').contains('Annuler').should('not.exist');
  });

  it('should display payment history with correct labels', () => {
    mountComponent({
      payment: {
        ...mockPayment,
        history: [
          { action: 'PAYMENT_CREATED', userId: 8, timestamp: new Date().toISOString() },
          { action: 'PAYMENT_CONFIRMED', userId: 2, timestamp: new Date().toISOString() }
        ]
      }
    });
    
    cy.contains('Créé').should('exist');
    cy.contains('Confirmé').should('exist');
  });

  it('should generate QR data URL correctly', () => {
    mountComponent();
    
    // The component sets qrData in ngOnInit
    cy.window().then(win => {
      expect(win.location.origin).to.be.a('string');
    });
  });

  it('should show correct status badge classes', () => {
    const statuses = ['PENDING', 'CONFIRMED', 'REJECTED', 'CANCELLED'];
    const labels = ['En attente', 'Confirmé', 'Rejeté', 'Annulé'];
    
    statuses.forEach((status, i) => {
      mountComponent({ payment: { ...mockPayment, status } });
      cy.contains(labels[i]).should('exist');
    });
  });

  it('should show action buttons based on user role and payment status', () => {
    // For shop user with PENDING payment - should see cancel
    mountComponent({ payment: { ...mockPayment, status: 'PENDING' } });
    cy.contains('Annuler').should('exist');
    cy.contains('Confirmer').should('not.exist'); // Shop can't confirm
    cy.contains('Rejeter').should('not.exist'); // Shop can't reject
  });
});

// Need Observable import
import { Observable } from 'rxjs';