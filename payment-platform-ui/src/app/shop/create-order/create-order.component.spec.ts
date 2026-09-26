// @ts-nocheck
/**
 * Tests du composant CreateOrderComponent (shop).
 * Perimetre : instanciation et blocs filteredProducts, addProduct / removeLine, totals, canSubmit, submit (voir blocs describe/it).
 * Moyens : TestBed + fixture, stubs jasmine, client HTTP de test, observables RxJS mockes.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { CreateOrderComponent } from './create-order.component';
import { OrderService } from '../../services/order.service';
import { OrganizationService } from '../../services/organization.service';
import { StockService } from '../../services/stock.service';
import { ToastService } from '../../services/toast.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('CreateOrderComponent (shop)', () => {
  let component: CreateOrderComponent;
  let fixture: ComponentFixture<CreateOrderComponent>;
  let orderService: jasmine.SpyObj<OrderService>;
  let orgService: jasmine.SpyObj<OrganizationService>;
  let stockService: jasmine.SpyObj<StockService>;
  let toast: jasmine.SpyObj<ToastService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    sessionStorage.setItem('user', JSON.stringify({ organizationId: 2 }));
    const orderSpy = jasmine.createSpyObj('OrderService', ['create']);
    const orgSpy = jasmine.createSpyObj('OrganizationService', ['listRelations', 'listSuppliers', 'listRelationsByShop']);
    const stockSpy = jasmine.createSpyObj('StockService', ['getProducts', 'getProductsBySupplier']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    orgSpy.listRelations.and.returnValue(of([]));
    orgSpy.listRelationsByShop.and.returnValue(of([]));
    orgSpy.listSuppliers.and.returnValue(of([]));
    stockSpy.getProducts.and.returnValue(of([]));
    stockSpy.getProductsBySupplier.and.returnValue(of([]));

    TestBed.configureTestingModule({
    declarations: [CreateOrderComponent],
    imports: [FormsModule],
    providers: [
        { provide: OrderService, useValue: orderSpy },
        { provide: OrganizationService, useValue: orgSpy },
        { provide: StockService, useValue: stockSpy },
        { provide: ToastService, useValue: toastSpy },
        { provide: Router, useValue: routerSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(CreateOrderComponent);
    component = fixture.componentInstance;
    orderService = TestBed.inject(OrderService) as jasmine.SpyObj<OrderService>;
    orgService = TestBed.inject(OrganizationService) as jasmine.SpyObj<OrganizationService>;
    stockService = TestBed.inject(StockService) as jasmine.SpyObj<StockService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('filteredProducts', () => {
    it('should return all when no search', () => {
      component.products = [{ id: 1 } as any];
      component.searchQuery = '';
      expect(component.filteredProducts.length).toBe(1);
    });

    it('should filter by search', () => {
      component.products = [{ id: 1, name: 'Apple', sku: 'S1' } as any, { id: 2, name: 'Banana', sku: 'S2' } as any];
      component.searchQuery = 'apple';
      expect(component.filteredProducts.length).toBe(1);
    });
  });

  describe('addProduct / removeLine', () => {
    it('should add product', () => {
      component.orderLines = [];
      component.addProduct({ id: 1, unitPrice: 10, quantity: 5 } as any);
      expect(component.orderLines.length).toBe(1);
    });

    it('should increment quantity for existing', () => {
      component.orderLines = [{ product: { id: 1 } as any, quantity: 1, discount: 0 }];
      component.addProduct({ id: 1, unitPrice: 10, quantity: 5 } as any);
      expect(component.orderLines[0].quantity).toBe(2);
    });

    it('should remove line', () => {
      component.orderLines = [{ product: { id: 1 } as any, quantity: 1, discount: 0 }];
      component.removeLine(0);
      expect(component.orderLines.length).toBe(0);
    });
  });

  describe('totals', () => {
    it('should compute subtotal, tax, total', () => {
      component.orderLines = [{ product: { id: 1, unitPrice: 100 } as any, quantity: 2, discount: 0 }];
      expect(component.subtotal).toBe(200);
      expect(component.taxAmount).toBe(38);
      expect(component.total).toBe(238);
    });
  });

  describe('canSubmit', () => {
    it('should return true when valid', () => {
      component.selectedSupplierId = 1;
      component.orderLines = [{ product: { id: 1 } as any, quantity: 1, discount: 0 }];
      expect(component.canSubmit()).toBeTrue();
    });

    it('should return false when no supplier', () => {
      component.selectedSupplierId = '';
      component.orderLines = [{ product: { id: 1 } as any, quantity: 1, discount: 0 }];
      expect(component.canSubmit()).toBeFalse();
    });
  });

  describe('submit', () => {
    it('should create order', () => {
      orderService.create.and.returnValue(of({} as any));
      component.selectedSupplierId = 1;
      component.orderLines = [{ product: { id: 1, unitPrice: 10 } as any, quantity: 1, discount: 0 }];
      component.submit();
      expect(orderService.create).toHaveBeenCalled();
    });

    it('should handle error', () => {
      orderService.create.and.returnValue(throwError(() => ({ error: { message: 'Fail' } })));
      component.selectedSupplierId = 1;
      component.orderLines = [{ product: { id: 1, unitPrice: 10 } as any, quantity: 1, discount: 0 }];
      component.submit();
      expect(toast.error).toHaveBeenCalled();
      expect(component.creating).toBeFalse();
    });
  });

  describe('onSupplierChange', () => {
    it('should clear products and lines', () => {
      component.products = [{ id: 1 } as any];
      component.orderLines = [{ product: { id: 1 } as any, quantity: 1, discount: 0 }];
      component.onSupplierChange();
      expect(component.products.length).toBe(0);
      expect(component.orderLines.length).toBe(0);
    });

    it('should not load products when supplierId is 0', () => {
      component.selectedSupplierId = '';
      component.onSupplierChange();
      expect(stockService.getProductsBySupplier).not.toHaveBeenCalled();
    });

    it('should load products when supplier is selected', () => {
      stockService.getProductsBySupplier.and.returnValue(of([{ id: 1, supplierId: 1 } as any]));
      component.selectedSupplierId = 1;
      component.onSupplierChange();
      expect(stockService.getProductsBySupplier).toHaveBeenCalledWith(1, 'ACTIVE');
    });

    it('should clear searchQuery', () => {
      component.searchQuery = 'test';
      component.selectedSupplierId = 1;
      stockService.getProductsBySupplier.and.returnValue(of([]));
      component.onSupplierChange();
      expect(component.searchQuery).toBe('');
    });
  });

  describe('ngOnInit', () => {
    it('should load suppliers from relations', () => {
      orgService.listRelationsByShop.and.returnValue(of([
        { shopId: 2, supplierId: 10, status: 'ACTIVE' },
        { shopId: 2, supplierId: 20, status: 'ACTIVE' },
        { shopId: 3, supplierId: 30, status: 'ACTIVE' }
      ] as any));
      orgService.listSuppliers.and.returnValue(of([
        { id: 10, name: 'Sup10' },
        { id: 20, name: 'Sup20' },
        { id: 30, name: 'Sup30' }
      ] as any));
      component.ngOnInit();
      expect(component.shopId).toBe(2);
    });

    it('should handle no user in session', () => {
      sessionStorage.removeItem('user');
      component.ngOnInit();
      expect(component.shopId).toBe('');
    });

    it('should handle empty relations', () => {
      orgService.listRelationsByShop.and.returnValue(of([]));
      orgService.listSuppliers.and.returnValue(of([]));
      component.ngOnInit();
      expect(component.suppliers.length).toBe(0);
    });
  });

  describe('filteredProducts', () => {
    it('should filter by SKU', () => {
      component.products = [
        { id: 1, name: 'Product A', sku: 'SKU-001' } as any,
        { id: 2, name: 'Product B', sku: 'SKU-002' } as any
      ];
      component.searchQuery = 'sku-001';
      expect(component.filteredProducts.length).toBe(1);
    });
  });

  describe('canSubmit', () => {
    it('should return false when creating', () => {
      component.selectedSupplierId = 1;
      component.orderLines = [{ product: { id: 1 } as any, quantity: 1, discount: 0 }];
      component.creating = true;
      expect(component.canSubmit()).toBeFalse();
    });

    it('should return false when no order lines', () => {
      component.selectedSupplierId = 1;
      component.orderLines = [];
      expect(component.canSubmit()).toBeFalse();
    });
  });

  describe('submit', () => {
    it('should not submit when canSubmit is false', () => {
      component.selectedSupplierId = '';
      component.submit();
      expect(orderService.create).not.toHaveBeenCalled();
    });

    it('should navigate on success', () => {
      orderService.create.and.returnValue(of({} as any));
      component.selectedSupplierId = 1;
      component.shopId = 2;
      component.orderLines = [{ product: { id: 1, unitPrice: 10 } as any, quantity: 2, discount: 10 }];
      component.asapPayment = true;
      component.currency = 'EUR';
      component.notes = 'test note';
      component.submit();
      expect(orderService.create).toHaveBeenCalled();
      expect(router.navigate).toHaveBeenCalledWith(['/dashboard/shop/orders']);
    });

    it('should handle error without message', () => {
      orderService.create.and.returnValue(throwError(() => ({ error: {} })));
      component.selectedSupplierId = 1;
      component.orderLines = [{ product: { id: 1, unitPrice: 10 } as any, quantity: 1, discount: 0 }];
      component.submit();
      expect(toast.error).toHaveBeenCalledWith('Erreur lors de la création');
      expect(component.creating).toBeFalse();
    });
  });

  describe('subtotal / taxAmount / total', () => {
    it('should compute with discount', () => {
      component.orderLines = [
        { product: { id: 1, unitPrice: 100 } as any, quantity: 1, discount: 10 }
      ];
      expect(component.subtotal).toBe(90);
      expect(component.taxAmount).toBeCloseTo(17.1);
      expect(component.total).toBeCloseTo(107.1);
    });

    it('should handle empty order lines', () => {
      component.orderLines = [];
      expect(component.subtotal).toBe(0);
      expect(component.taxAmount).toBe(0);
      expect(component.total).toBe(0);
    });
  });
});
