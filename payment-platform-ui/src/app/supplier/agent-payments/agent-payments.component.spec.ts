import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { AgentPaymentsComponent } from './agent-payments.component';
import { PaymentService } from '../../services/payment.service';
import { LoginService } from '../../services/login.service';
import { ToastService } from '../../services/toast.service';
import { AgentPaymentSummary } from '../../models/agent-payment.model';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('AgentPaymentsComponent', () => {
  let component: AgentPaymentsComponent;
  let fixture: ComponentFixture<AgentPaymentsComponent>;
  let paymentService: jasmine.SpyObj<PaymentService>;
  let loginService: jasmine.SpyObj<LoginService>;
  let toast: jasmine.SpyObj<ToastService>;

  const mockSummary: AgentPaymentSummary = {
    userId: 1, username: 'agent1', paymentCount: 5, totalAmount: 500,
    confirmedTotal: 300, currency: 'TND', payments: []
  };

  beforeEach(() => {
    const paymentSpy = jasmine.createSpyObj('PaymentService', ['getAgentSummary']);
    const loginSpy = jasmine.createSpyObj('LoginService', ['getCurrentUser']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    loginSpy.getCurrentUser.and.returnValue({ organizationId: 1 } as any);
    paymentSpy.getAgentSummary.and.returnValue(of([mockSummary]));

    TestBed.configureTestingModule({
    declarations: [AgentPaymentsComponent],
    imports: [],
    providers: [
        { provide: PaymentService, useValue: paymentSpy },
        { provide: LoginService, useValue: loginSpy },
        { provide: ToastService, useValue: toastSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(AgentPaymentsComponent);
    component = fixture.componentInstance;
    paymentService = TestBed.inject(PaymentService) as jasmine.SpyObj<PaymentService>;
    loginService = TestBed.inject(LoginService) as jasmine.SpyObj<LoginService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('computeTotals', () => {
    it('should compute totals', () => {
      component.summaries = [mockSummary, { ...mockSummary, userId: 2, totalAmount: 200, confirmedTotal: 150, paymentCount: 3 }];
      component.computeTotals();
      expect(component.grandTotal).toBe(700);
      expect(component.grandConfirmedTotal).toBe(450);
      expect(component.totalPayments).toBe(8);
    });
  });

  describe('toggleAgent', () => {
    it('should expand agent', () => {
      component.toggleAgent(1);
      expect(component.expandedAgent).toBe(1);
    });

    it('should collapse if same agent', () => {
      component.expandedAgent = 1;
      component.toggleAgent(1);
      expect(component.expandedAgent).toBeNull();
    });
  });

  describe('getAgentPayments', () => {
    it('should return all payments when no filter', () => {
      const agent = { payments: [{ status: 'PENDING' }, { status: 'CONFIRMED' }] } as any;
      component.statusFilter = '';
      expect(component.getAgentPayments(agent).length).toBe(2);
    });

    it('should filter by status', () => {
      const agent = { payments: [{ status: 'PENDING' }, { status: 'CONFIRMED' }] } as any;
      component.statusFilter = 'PENDING';
      expect(component.getAgentPayments(agent).length).toBe(1);
    });
  });

  describe('statusLabel', () => {
    it('should return correct labels', () => {
      expect(component.statusLabel('PENDING')).toBe('En attente');
      expect(component.statusLabel('CONFIRMED')).toBe('Confirmé');
      expect(component.statusLabel('REJECTED')).toBe('Rejeté');
      expect(component.statusLabel('CANCELLED')).toBe('Annulé');
      expect(component.statusLabel('UNKNOWN')).toBe('UNKNOWN');
    });
  });

  describe('load', () => {
    it('should not load without supplierId', () => {
      loginService.getCurrentUser.and.returnValue(null as any);
      component.supplierId = '';
      component.load();
      expect(paymentService.getAgentSummary).not.toHaveBeenCalled();
    });

    it('should not load without fromDate', () => {
      component.supplierId = 1;
      component.fromDate = '';
      component.toDate = '2024-01-31';
      component.load();
      expect(paymentService.getAgentSummary).not.toHaveBeenCalled();
    });

    it('should not load without toDate', () => {
      component.supplierId = 1;
      component.fromDate = '2024-01-01';
      component.toDate = '';
      component.load();
      expect(paymentService.getAgentSummary).not.toHaveBeenCalled();
    });

    it('should load data successfully', () => {
      component.supplierId = 1;
      component.fromDate = '2024-01-01';
      component.toDate = '2024-01-31';
      component.load();
      expect(component.summaries.length).toBe(1);
      expect(component.loading).toBeFalse();
    });

    it('should handle error', () => {
      paymentService.getAgentSummary.and.returnValue(throwError(() => ({})));
      component.supplierId = 1;
      component.fromDate = '2024-01-01';
      component.toDate = '2024-01-31';
      component.load();
      expect(toast.error).toHaveBeenCalled();
      expect(component.loading).toBeFalse();
    });
  });

  describe('ngOnInit', () => {
    it('should set dates and supplierId from current user', () => {
      component.ngOnInit();
      expect(component.supplierId).toBe(1);
      expect(component.fromDate).toBeTruthy();
      expect(component.toDate).toBeTruthy();
    });

    it('should handle null user', () => {
      loginService.getCurrentUser.and.returnValue(null as any);
      component.ngOnInit();
      expect(component.supplierId).toBe(0);
    });
  });

  describe('formatDate', () => {
    it('should format date as YYYY-MM-DD', () => {
      const result = component['formatDate'](new Date(2024, 0, 15));
      expect(result).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    });

    it('should format today date', () => {
      const result = component['formatDate'](new Date());
      expect(result).toBeTruthy();
      expect(result.split('-').length).toBe(3);
    });
  });

  describe('computeTotals', () => {
    it('should handle empty summaries', () => {
      component.summaries = [];
      component.computeTotals();
      expect(component.grandTotal).toBe(0);
      expect(component.grandConfirmedTotal).toBe(0);
      expect(component.totalPayments).toBe(0);
    });
  });
});
