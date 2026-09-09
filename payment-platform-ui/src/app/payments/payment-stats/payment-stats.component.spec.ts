import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';
import { PaymentStatsComponent } from './payment-stats.component';
import { PaymentService } from '../../services/payment.service';

describe('PaymentStatsComponent', () => {
  let component: PaymentStatsComponent;
  let fixture: ComponentFixture<PaymentStatsComponent>;
  let paymentService: jasmine.SpyObj<PaymentService>;

  beforeEach(() => {
    const psSpy = jasmine.createSpyObj('PaymentService', ['getStats']);
    psSpy.getStats.and.returnValue(of({ totalPayments: 10, totalAmount: 5000 } as any));

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [PaymentStatsComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: PaymentService, useValue: psSpy }
      ]
    });
    fixture = TestBed.createComponent(PaymentStatsComponent);
    component = fixture.componentInstance;
    paymentService = TestBed.inject(PaymentService) as jasmine.SpyObj<PaymentService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load stats on init', () => {
    component.ngOnInit();
    expect(component.stats).toBeTruthy();
    expect(component.loading).toBeFalse();
  });

  it('should handle load error', () => {
    paymentService.getStats.and.returnValue(throwError(() => new Error('fail')));
    component.ngOnInit();
    expect(component.loading).toBeFalse();
    expect(component.stats).toBeNull();
  });
});
