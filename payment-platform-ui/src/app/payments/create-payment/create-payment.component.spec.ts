import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { CreatePaymentComponent } from './create-payment.component';

describe('CreatePaymentComponent', () => {
  let component: CreatePaymentComponent;
  let fixture: ComponentFixture<CreatePaymentComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule],
      declarations: [CreatePaymentComponent]
    });
    fixture = TestBed.createComponent(CreatePaymentComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have initial state', () => {
    expect(component.shopId).toBe(0);
    expect(component.supplierId).toBe(0);
    expect(component.amount).toBe(0);
    expect(component.currency).toBe('TND');
    expect(component.creating).toBeFalse();
  });
});
