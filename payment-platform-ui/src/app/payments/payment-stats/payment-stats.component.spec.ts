import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { PaymentStatsComponent } from './payment-stats.component';

describe('PaymentStatsComponent', () => {
  let component: PaymentStatsComponent;
  let fixture: ComponentFixture<PaymentStatsComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [PaymentStatsComponent]
    });
    fixture = TestBed.createComponent(PaymentStatsComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have initial state', () => {
    expect(component.stats).toBeNull();
    expect(component.loading).toBeTrue();
  });
});
