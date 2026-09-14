import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { AddProductComponent } from './add-product.component';
import { StockService } from '../../services/stock.service';
import { ToastService } from '../../services/toast.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('AddProductComponent', () => {
  let component: AddProductComponent;
  let fixture: ComponentFixture<AddProductComponent>;
  let stockService: jasmine.SpyObj<StockService>;
  let toast: jasmine.SpyObj<ToastService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    const stockSpy = jasmine.createSpyObj('StockService', ['createProduct']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
    declarations: [AddProductComponent],
    imports: [FormsModule],
    providers: [
        { provide: StockService, useValue: stockSpy },
        { provide: ToastService, useValue: toastSpy },
        { provide: Router, useValue: routerSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(AddProductComponent);
    component = fixture.componentInstance;
    stockService = TestBed.inject(StockService) as jasmine.SpyObj<StockService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('getCurrencySymbol', () => {
    it('should return correct symbols', () => {
      expect(component.getCurrencySymbol('TND')).toBe('DT');
      expect(component.getCurrencySymbol('EUR')).toBe('€');
      expect(component.getCurrencySymbol('USD')).toBe('$');
      expect(component.getCurrencySymbol('XYZ')).toBe('XYZ');
    });
  });

  describe('validate', () => {
    it('should pass with valid data', () => {
      component.form = { name: 'Product', sku: 'SKU-1', description: '', unitPrice: 10, currency: 'TND', quantity: 0, minQuantity: 0 };
      expect(component.validate()).toBeTrue();
      expect(Object.keys(component.errors).length).toBe(0);
    });

    it('should fail when name is empty', () => {
      component.form = { name: '', sku: 'SKU-1', description: '', unitPrice: 10, currency: 'TND', quantity: 0, minQuantity: 0 };
      expect(component.validate()).toBeFalse();
      expect(component.errors['name']).toBeTruthy();
    });

    it('should fail when name is too short', () => {
      component.form = { name: 'P', sku: 'SKU-1', description: '', unitPrice: 10, currency: 'TND', quantity: 0, minQuantity: 0 };
      expect(component.validate()).toBeFalse();
    });

    it('should fail when sku is empty', () => {
      component.form = { name: 'Product', sku: '', description: '', unitPrice: 10, currency: 'TND', quantity: 0, minQuantity: 0 };
      expect(component.validate()).toBeFalse();
      expect(component.errors['sku']).toBeTruthy();
    });

    it('should fail when sku is too short', () => {
      component.form = { name: 'Product', sku: 'S', description: '', unitPrice: 10, currency: 'TND', quantity: 0, minQuantity: 0 };
      expect(component.validate()).toBeFalse();
    });

    it('should fail when unitPrice is 0', () => {
      component.form = { name: 'Product', sku: 'SKU-1', description: '', unitPrice: 0, currency: 'TND', quantity: 0, minQuantity: 0 };
      expect(component.validate()).toBeFalse();
      expect(component.errors['unitPrice']).toBeTruthy();
    });

    it('should fail when unitPrice is negative', () => {
      component.form = { name: 'Product', sku: 'SKU-1', description: '', unitPrice: -5, currency: 'TND', quantity: 0, minQuantity: 0 };
      expect(component.validate()).toBeFalse();
    });

    it('should fail when quantity is negative', () => {
      component.form = { name: 'Product', sku: 'SKU-1', description: '', unitPrice: 10, currency: 'TND', quantity: -1, minQuantity: 0 };
      expect(component.validate()).toBeFalse();
      expect(component.errors['quantity']).toBeTruthy();
    });

    it('should fail when minQuantity is negative', () => {
      component.form = { name: 'Product', sku: 'SKU-1', description: '', unitPrice: 10, currency: 'TND', quantity: 0, minQuantity: -1 };
      expect(component.validate()).toBeFalse();
      expect(component.errors['minQuantity']).toBeTruthy();
    });
  });

  describe('onSubmit', () => {
    it('should create product on valid form', () => {
      stockService.createProduct.and.returnValue(of({} as any));
      component.form = { name: 'Product', sku: 'SKU-1', description: '', unitPrice: 10, currency: 'TND', quantity: 0, minQuantity: 0 };
      component.onSubmit();
      expect(component.success).toBeTrue();
      expect(toast.success).toHaveBeenCalled();
    });

    it('should not submit invalid form', () => {
      component.onSubmit();
      expect(stockService.createProduct).not.toHaveBeenCalled();
    });

    it('should handle error', () => {
      stockService.createProduct.and.returnValue(throwError(() => ({ error: { message: 'Fail' } })));
      component.form = { name: 'Product', sku: 'SKU-1', description: '', unitPrice: 10, currency: 'TND', quantity: 0, minQuantity: 0 };
      component.onSubmit();
      expect(toast.error).toHaveBeenCalledWith('Fail');
    });

    it('should handle error without message', () => {
      stockService.createProduct.and.returnValue(throwError(() => ({ error: {} })));
      component.form = { name: 'Product', sku: 'SKU-1', description: '', unitPrice: 10, currency: 'TND', quantity: 0, minQuantity: 0 };
      component.onSubmit();
      expect(toast.error).toHaveBeenCalledWith('Erreur lors de la création');
    });
  });

  describe('resetForm', () => {
    it('should reset form and errors', () => {
      component.form = { name: 'Test', sku: 'SKU', description: '', unitPrice: 10, currency: 'TND', quantity: 5, minQuantity: 2 };
      component.errors = { name: 'Error' };
      component.success = true;
      component.resetForm();
      expect(component.form.name).toBe('');
      expect(component.errors).toEqual({});
      expect(component.success).toBeFalse();
    });
  });

  describe('goBack', () => {
    it('should navigate to stock page', () => {
      component.goBack();
      expect(router.navigate).toHaveBeenCalledWith(['/supplier/stock']);
    });
  });
});
