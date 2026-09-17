// @ts-nocheck
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { StockDashboardComponent } from './stock-dashboard.component';
import { StockService } from '../../services/stock.service';
import { ToastService } from '../../services/toast.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('StockDashboardComponent', () => {
  let component: StockDashboardComponent;
  let fixture: ComponentFixture<StockDashboardComponent>;
  let stockService: jasmine.SpyObj<StockService>;
  let toast: jasmine.SpyObj<ToastService>;

  const mockProduct = {
    id: 1, supplierId: 1, name: 'Product A', sku: 'SKU-001', description: '',
    unitPrice: 10, currency: 'TND', quantity: 5, minQuantity: 10, reservedQty: 0,
    categoryId: null, familyId: null, unit: 'unite', status: 'ACTIVE', createdAt: '', updatedAt: ''
  } as any;

  beforeEach(() => {
    const stockSpy = jasmine.createSpyObj('StockService', ['getProducts', 'getStockMovements', 'createStockMovement']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    stockSpy.getProducts.and.returnValue(of([]));
    stockSpy.getStockMovements.and.returnValue(of([]));

    TestBed.configureTestingModule({
    declarations: [StockDashboardComponent],
    schemas: [NO_ERRORS_SCHEMA],
    imports: [FormsModule],
    providers: [
        { provide: StockService, useValue: stockSpy },
        { provide: ToastService, useValue: toastSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(StockDashboardComponent);
    component = fixture.componentInstance;
    stockService = TestBed.inject(StockService) as jasmine.SpyObj<StockService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('loadData', () => {
    it('should load products', () => {
      stockService.getProducts.and.returnValue(of([mockProduct]));
      component.loadData();
      expect(component.products.length).toBe(1);
      expect(component.loading).toBeFalse();
    });

    it('should handle error', () => {
      stockService.getProducts.and.returnValue(throwError(() => ({ error: { message: 'Fail' } })));
      component.loadData();
      expect(toast.error).toHaveBeenCalledWith('Fail');
      expect(component.loading).toBeFalse();
    });

    it('should handle error without message', () => {
      stockService.getProducts.and.returnValue(throwError(() => ({})));
      component.loadData();
      expect(toast.error).toHaveBeenCalledWith('Erreur de chargement');
    });
  });

  describe('computeStats', () => {
    it('should compute stats from products', () => {
      component.products = [
        { ...mockProduct, id: 1, quantity: 5, minQuantity: 10, unitPrice: 10 },
        { ...mockProduct, id: 2, quantity: 20, minQuantity: 10, unitPrice: 20 },
        { ...mockProduct, id: 3, quantity: 0, minQuantity: 10, unitPrice: 15 }
      ];
      component.computeStats();
      expect(component.totalProducts).toBe(3);
      expect(component.totalQuantity).toBe(25);
      expect(component.totalValue).toBe(450);
      expect(component.lowStock).toBe(1);
      expect(component.outOfStock).toBe(1);
    });

    it('should handle empty products', () => {
      component.products = [];
      component.computeStats();
      expect(component.totalProducts).toBe(0);
      expect(component.totalQuantity).toBe(0);
      expect(component.totalValue).toBe(0);
    });
  });

  describe('getStockStatus', () => {
    it('should return OUT_OF_STOCK', () => {
      expect(component.getStockStatus({ ...mockProduct, quantity: 0 })).toBe('OUT_OF_STOCK');
    });

    it('should return LOW', () => {
      expect(component.getStockStatus({ ...mockProduct, quantity: 5, minQuantity: 10 })).toBe('LOW');
    });

    it('should return OK', () => {
      expect(component.getStockStatus({ ...mockProduct, quantity: 20, minQuantity: 10 })).toBe('OK');
    });
  });

  describe('movementTypeLabel', () => {
    it('should return correct labels', () => {
      expect(component.movementTypeLabel('IN')).toBe('Entrée');
      expect(component.movementTypeLabel('OUT')).toBe('Sortie');
      expect(component.movementTypeLabel('ADJUSTMENT')).toBe('Ajustement');
      expect(component.movementTypeLabel('UNKNOWN')).toBe('UNKNOWN');
    });
  });

  describe('movementTypeClass', () => {
    it('should return correct classes', () => {
      expect(component.movementTypeClass('IN')).toBe('movement-in');
      expect(component.movementTypeClass('OUT')).toBe('movement-out');
      expect(component.movementTypeClass('ADJUSTMENT')).toBe('movement-adjust');
      expect(component.movementTypeClass('UNKNOWN')).toBe('');
    });
  });

  describe('openMovement / closeMovement', () => {
    it('should open and close modal', () => {
      component.openMovement(mockProduct, 'IN');
      expect(component.showMovementModal).toBeTrue();
      expect(component.selectedProduct).toBe(mockProduct);
      expect(component.movementType).toBe('IN');
      expect(component.movementQty).toBe(0);
      expect(component.movementRef).toBe('');
      expect(component.movementNotes).toBe('');
      component.closeMovement();
      expect(component.showMovementModal).toBeFalse();
      expect(component.selectedProduct).toBeNull();
    });
  });

  describe('submitMovement', () => {
    it('should not submit when no product', () => {
      component.selectedProduct = null;
      component.submitMovement();
      expect(stockService.createStockMovement).not.toHaveBeenCalled();
    });

    it('should not submit when qty is 0', () => {
      component.selectedProduct = mockProduct;
      component.movementQty = 0;
      component.submitMovement();
      expect(stockService.createStockMovement).not.toHaveBeenCalled();
    });

    it('should not submit when qty is negative', () => {
      component.selectedProduct = mockProduct;
      component.movementQty = -1;
      component.submitMovement();
      expect(stockService.createStockMovement).not.toHaveBeenCalled();
    });

    it('should submit valid movement', () => {
      stockService.createStockMovement.and.returnValue(of({} as any));
      component.selectedProduct = mockProduct;
      component.movementQty = 10;
      component.movementRef = 'REF-001';
      component.movementNotes = 'notes';
      component.submitMovement();
      expect(toast.success).toHaveBeenCalledWith('Mouvement de stock enregistré');
      expect(component.showMovementModal).toBeFalse();
    });

    it('should handle submit error', () => {
      stockService.createStockMovement.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
      component.selectedProduct = mockProduct;
      component.movementQty = 10;
      component.submitMovement();
      expect(toast.error).toHaveBeenCalledWith('Err');
      expect(component.savingMovement).toBeFalse();
    });

    it('should handle submit error without message', () => {
      stockService.createStockMovement.and.returnValue(throwError(() => ({})));
      component.selectedProduct = mockProduct;
      component.movementQty = 10;
      component.submitMovement();
      expect(toast.error).toHaveBeenCalledWith('Erreur');
    });
  });

  describe('openHistory / closeHistory', () => {
    it('should open and close history modal', () => {
      stockService.getStockMovements.and.returnValue(of([{ id: 1 } as any]));
      component.openHistory(mockProduct);
      expect(component.showHistoryModal).toBeTrue();
      expect(component.historyProduct).toBe(mockProduct);
      expect(component.loadingHistory).toBeFalse();
      expect(component.historyMovements.length).toBe(1);
      component.closeHistory();
      expect(component.showHistoryModal).toBeFalse();
      expect(component.historyProduct).toBeNull();
      expect(component.historyMovements.length).toBe(0);
    });

    it('should handle history load error', () => {
      stockService.getStockMovements.and.returnValue(throwError(() => new Error('fail')));
      component.openHistory(mockProduct);
      expect(component.historyMovements.length).toBe(0);
      expect(component.loadingHistory).toBeFalse();
    });
  });
});
