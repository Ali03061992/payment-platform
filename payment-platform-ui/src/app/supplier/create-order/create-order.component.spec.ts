import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { SupplierCreateOrderComponent } from './create-order.component';
import { OrderService } from '../../services/order.service';
import { OrganizationService } from '../../services/organization.service';
import { StockService } from '../../services/stock.service';
import { ToastService } from '../../services/toast.service';

describe('SupplierCreateOrderComponent', () => {
  let component: SupplierCreateOrderComponent;
  let fixture: ComponentFixture<SupplierCreateOrderComponent>;
  let orderService: jasmine.SpyObj<OrderService>;
  let orgService: jasmine.SpyObj<OrganizationService>;
  let stockService: jasmine.SpyObj<StockService>;
  let toast: jasmine.SpyObj<ToastService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    sessionStorage.setItem('user', JSON.stringify({ organizationId: 1 }));
    const orderSpy = jasmine.createSpyObj('OrderService', ['create']);
    const orgSpy = jasmine.createSpyObj('OrganizationService', ['listRelations', 'listShops']);
    const stockSpy = jasmine.createSpyObj('StockService', ['getProducts']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    orgSpy.listRelations.and.returnValue(of([]));
    orgSpy.listShops.and.returnValue(of([]));
    stockSpy.getProducts.and.returnValue(of([]));

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, FormsModule],
      declarations: [SupplierCreateOrderComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: OrderService, useValue: orderSpy },
        { provide: OrganizationService, useValue: orgSpy },
        { provide: StockService, useValue: stockSpy },
        { provide: ToastService, useValue: toastSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });
    fixture = TestBed.createComponent(SupplierCreateOrderComponent);
    component = fixture.componentInstance;
    orderService = TestBed.inject(OrderService) as jasmine.SpyObj<OrderService>;
    orgService = TestBed.inject(OrganizationService) as jasmine.SpyObj<OrganizationService>;
    stockService = TestBed.inject(StockService) as jasmine.SpyObj<StockService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  afterEach(() => sessionStorage.clear());

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should load shops and set supplierId', () => {
      orgService.listRelations.and.returnValue(of([
        { supplierId: 1, shopId: 10, status: 'ACTIVE' },
        { supplierId: 1, shopId: 20, status: 'ACTIVE' },
        { supplierId: 2, shopId: 10, status: 'ACTIVE' },
        { supplierId: 1, shopId: 30, status: 'INACTIVE' }
      ] as any));
      orgService.listShops.and.returnValue(of([
        { id: 10, name: 'Shop10' } as any,
        { id: 20, name: 'Shop20' } as any,
        { id: 30, name: 'Shop30' } as any,
        { id: 40, name: 'Shop40' } as any
      ]));
      component.ngOnInit();
      expect(component.supplierId).toBe(1);
      expect(component.shops.length).toBe(2);
    });

    it('should handle no user in session', () => {
      sessionStorage.clear();
      component.ngOnInit();
      expect(component.supplierId).toBe(0);
    });

    it('should handle user with no organizationId', () => {
      sessionStorage.setItem('user', JSON.stringify({ username: 'test' }));
      component.ngOnInit();
      expect(component.supplierId).toBe(0);
    });
  });

  describe('ngOnDestroy', () => {
    it('should unsubscribe', () => {
      component.ngOnInit();
      expect(() => component.ngOnDestroy()).not.toThrow();
    });
  });

  describe('onShopChange', () => {
    it('should clear products and lines', () => {
      component.products = [{ id: 1 } as any];
      component.orderLines = [{ product: { id: 1 } as any, quantity: 1, discount: 0 }];
      component.selectedShopId = 0;
      component.onShopChange();
      expect(component.products.length).toBe(0);
      expect(component.orderLines.length).toBe(0);
    });

    it('should load products when shop selected', () => {
      stockService.getProducts.and.returnValue(of([{ id: 1 } as any]));
      component.selectedShopId = 10;
      component.onShopChange();
      expect(stockService.getProducts).toHaveBeenCalledWith('ACTIVE');
    });
  });

  describe('filteredProducts', () => {
    it('should return all when no search', () => {
      component.products = [{ id: 1, name: 'A', sku: 'S1' } as any, { id: 2, name: 'B', sku: 'S2' } as any];
      component.searchQuery = '';
      expect(component.filteredProducts.length).toBe(2);
    });

    it('should filter by name', () => {
      component.products = [{ id: 1, name: 'Apple', sku: 'S1' } as any, { id: 2, name: 'Banana', sku: 'S2' } as any];
      component.searchQuery = 'apple';
      expect(component.filteredProducts.length).toBe(1);
    });

    it('should filter by sku', () => {
      component.products = [{ id: 1, name: 'Apple', sku: 'S1' } as any, { id: 2, name: 'Banana', sku: 'S2' } as any];
      component.searchQuery = 's2';
      expect(component.filteredProducts.length).toBe(1);
    });
  });

  describe('addProduct / removeLine', () => {
    it('should add new product to order lines', () => {
      component.orderLines = [];
      component.addProduct({ id: 1, unitPrice: 10 } as any);
      expect(component.orderLines.length).toBe(1);
      expect(component.orderLines[0].quantity).toBe(1);
      expect(component.orderLines[0].discount).toBe(0);
    });

    it('should increment quantity for existing product', () => {
      component.orderLines = [{ product: { id: 1 } as any, quantity: 1, discount: 0 }];
      component.addProduct({ id: 1, unitPrice: 10 } as any);
      expect(component.orderLines[0].quantity).toBe(2);
    });

    it('should remove line by index', () => {
      component.orderLines = [
        { product: { id: 1 } as any, quantity: 1, discount: 0 },
        { product: { id: 2 } as any, quantity: 1, discount: 0 }
      ];
      component.removeLine(0);
      expect(component.orderLines.length).toBe(1);
      expect(component.orderLines[0].product.id).toBe(2);
    });
  });

  describe('subtotal / taxAmount / total', () => {
    it('should compute totals with discount', () => {
      component.orderLines = [
        { product: { id: 1, unitPrice: 100 } as any, quantity: 2, discount: 10 }
      ];
      expect(component.subtotal).toBe(180);
      expect(component.taxAmount).toBeCloseTo(34.2);
      expect(component.total).toBeCloseTo(214.2);
    });

    it('should compute totals with no discount', () => {
      component.orderLines = [
        { product: { id: 1, unitPrice: 50 } as any, quantity: 3, discount: 0 }
      ];
      expect(component.subtotal).toBe(150);
      expect(component.taxAmount).toBeCloseTo(28.5);
      expect(component.total).toBeCloseTo(178.5);
    });

    it('should return 0 for empty lines', () => {
      component.orderLines = [];
      expect(component.subtotal).toBe(0);
      expect(component.taxAmount).toBe(0);
      expect(component.total).toBe(0);
    });
  });

  describe('canSubmit', () => {
    it('should return true when valid', () => {
      component.selectedShopId = 1;
      component.orderLines = [{ product: { id: 1 } as any, quantity: 1, discount: 0 }];
      expect(component.canSubmit()).toBeTrue();
    });

    it('should return false when no shop', () => {
      component.selectedShopId = 0;
      component.orderLines = [{ product: { id: 1 } as any, quantity: 1, discount: 0 }];
      expect(component.canSubmit()).toBeFalse();
    });

    it('should return false when no lines', () => {
      component.selectedShopId = 1;
      component.orderLines = [];
      expect(component.canSubmit()).toBeFalse();
    });

    it('should return false when creating', () => {
      component.selectedShopId = 1;
      component.orderLines = [{ product: { id: 1 } as any, quantity: 1, discount: 0 }];
      component.creating = true;
      expect(component.canSubmit()).toBeFalse();
    });
  });

  describe('submit', () => {
    it('should not submit when cannot submit', () => {
      component.selectedShopId = 0;
      component.submit();
      expect(orderService.create).not.toHaveBeenCalled();
    });

    it('should submit valid order', () => {
      orderService.create.and.returnValue(of({ id: 1 } as any));
      component.supplierId = 1;
      component.selectedShopId = 10;
      component.asapPayment = true;
      component.currency = 'USD';
      component.notes = 'test notes';
      component.orderLines = [{ product: { id: 5, unitPrice: 20 } as any, quantity: 3, discount: 5 }];
      component.submit();
      expect(orderService.create).toHaveBeenCalled();
      expect(toast.success).toHaveBeenCalledWith('Commande créée avec succès');
      expect(router.navigate).toHaveBeenCalledWith(['/dashboard/supplier/orders']);
    });

    it('should handle submit error', () => {
      orderService.create.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
      component.supplierId = 1;
      component.selectedShopId = 10;
      component.orderLines = [{ product: { id: 5 } as any, quantity: 1, discount: 0 }];
      component.submit();
      expect(toast.error).toHaveBeenCalledWith('Err');
      expect(component.creating).toBeFalse();
    });

    it('should handle submit error without message', () => {
      orderService.create.and.returnValue(throwError(() => ({})));
      component.supplierId = 1;
      component.selectedShopId = 10;
      component.orderLines = [{ product: { id: 5 } as any, quantity: 1, discount: 0 }];
      component.submit();
      expect(toast.error).toHaveBeenCalledWith('Erreur lors de la création');
    });
  });
});
