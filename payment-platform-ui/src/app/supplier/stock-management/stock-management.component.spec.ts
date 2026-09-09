import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { StockManagementComponent } from './stock-management.component';
import { StockService } from '../../services/stock.service';
import { ToastService } from '../../services/toast.service';

describe('StockManagementComponent', () => {
  let component: StockManagementComponent;
  let fixture: ComponentFixture<StockManagementComponent>;
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
      imports: [HttpClientTestingModule, FormsModule],
      declarations: [StockManagementComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: StockService, useValue: stockSpy },
        { provide: ToastService, useValue: toastSpy }
      ]
    });
    fixture = TestBed.createComponent(StockManagementComponent);
    component = fixture.componentInstance;
    stockService = TestBed.inject(StockService) as jasmine.SpyObj<StockService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('loadData', () => {
    it('should load products and apply filters', () => {
      stockService.getProducts.and.returnValue(of([mockProduct]));
      stockService.getStockMovements.and.returnValue(of([{ id: 1 } as any]));
      component.loadData();
      expect(component.products.length).toBe(1);
      expect(component.loading).toBeFalse();
      expect(component.recentMovements.length).toBe(1);
    });

    it('should handle load error', () => {
      stockService.getProducts.and.returnValue(throwError(() => new Error('fail')));
      component.loadData();
      expect(component.products.length).toBe(0);
      expect(component.loading).toBeFalse();
    });

    it('should handle movements error', () => {
      stockService.getProducts.and.returnValue(of([mockProduct]));
      stockService.getStockMovements.and.returnValue(throwError(() => new Error('fail')));
      component.loadData();
      expect(component.loading).toBeFalse();
    });
  });

  describe('getStockStatus', () => {
    it('should return OUT_OF_STOCK when quantity is 0', () => {
      expect(component.getStockStatus({ ...mockProduct, quantity: 0 })).toBe('OUT_OF_STOCK');
    });

    it('should return LOW when quantity <= minQuantity', () => {
      expect(component.getStockStatus({ ...mockProduct, quantity: 5, minQuantity: 10 })).toBe('LOW');
    });

    it('should return OK when quantity > minQuantity', () => {
      expect(component.getStockStatus({ ...mockProduct, quantity: 20, minQuantity: 10 })).toBe('OK');
    });
  });

  describe('getStockPercent', () => {
    it('should return 100 when minQuantity is 0', () => {
      expect(component.getStockPercent({ ...mockProduct, minQuantity: 0, quantity: 5 })).toBe(100);
    });

    it('should calculate percentage', () => {
      expect(component.getStockPercent({ ...mockProduct, quantity: 15, minQuantity: 10 })).toBe(50);
    });

    it('should cap at 100', () => {
      expect(component.getStockPercent({ ...mockProduct, quantity: 100, minQuantity: 10 })).toBe(100);
    });
  });

  describe('applyFilters', () => {
    it('should filter by search term in name', () => {
      component.products = [
        { ...mockProduct, id: 1, name: 'Product A', sku: 'SKU-001' },
        { ...mockProduct, id: 2, name: 'Product B', sku: 'SKU-002' }
      ];
      component.searchTerm = 'Product A';
      component.applyFilters();
      expect(component.filteredProducts.length).toBe(1);
    });

    it('should filter by search term in sku', () => {
      component.products = [
        { ...mockProduct, id: 1, name: 'Product A', sku: 'SKU-001' },
        { ...mockProduct, id: 2, name: 'Product B', sku: 'SKU-002' }
      ];
      component.searchTerm = 'SKU-002';
      component.applyFilters();
      expect(component.filteredProducts.length).toBe(1);
    });

    it('should filter by status LOW', () => {
      component.products = [
        { ...mockProduct, id: 1, quantity: 5, minQuantity: 10 },
        { ...mockProduct, id: 2, quantity: 20, minQuantity: 10 }
      ];
      component.filterStatus = 'LOW';
      component.applyFilters();
      expect(component.filteredProducts.length).toBe(1);
    });

    it('should filter by status OUT_OF_STOCK', () => {
      component.products = [
        { ...mockProduct, id: 1, quantity: 0, minQuantity: 10 },
        { ...mockProduct, id: 2, quantity: 20, minQuantity: 10 }
      ];
      component.filterStatus = 'OUT_OF_STOCK';
      component.applyFilters();
      expect(component.filteredProducts.length).toBe(1);
    });

    it('should filter by status OK', () => {
      component.products = [
        { ...mockProduct, id: 1, quantity: 5, minQuantity: 10 },
        { ...mockProduct, id: 2, quantity: 20, minQuantity: 10 }
      ];
      component.filterStatus = 'OK';
      component.applyFilters();
      expect(component.filteredProducts.length).toBe(1);
    });

    it('should sort by quantity ascending', () => {
      component.products = [
        { ...mockProduct, id: 1, quantity: 20, minQuantity: 10 },
        { ...mockProduct, id: 2, quantity: 5, minQuantity: 10 }
      ];
      component.sortBy = 'quantity';
      component.applyFilters();
      expect(component.filteredProducts[0].quantity).toBe(5);
    });

    it('should sort by name', () => {
      component.products = [
        { ...mockProduct, id: 1, name: 'Banana', quantity: 20, minQuantity: 10 },
        { ...mockProduct, id: 2, name: 'Apple', quantity: 5, minQuantity: 10 }
      ];
      component.sortBy = 'name';
      component.applyFilters();
      expect(component.filteredProducts[0].name).toBe('Apple');
    });

    it('should sort by lowStock default', () => {
      component.products = [
        { ...mockProduct, id: 1, quantity: 20, minQuantity: 10 },
        { ...mockProduct, id: 2, quantity: 5, minQuantity: 10 },
        { ...mockProduct, id: 3, quantity: 0, minQuantity: 10 }
      ];
      component.sortBy = 'lowStock';
      component.applyFilters();
      expect(component.filteredProducts.length).toBe(3);
    });

    it('should return all when no filters', () => {
      component.products = [{ ...mockProduct, id: 1 }, { ...mockProduct, id: 2 }];
      component.searchTerm = '';
      component.filterStatus = '';
      component.applyFilters();
      expect(component.filteredProducts.length).toBe(2);
    });
  });

  describe('computeStats', () => {
    it('should compute stats', () => {
      component.products = [
        { ...mockProduct, id: 1, quantity: 5, minQuantity: 10, unitPrice: 10 },
        { ...mockProduct, id: 2, quantity: 20, minQuantity: 10, unitPrice: 20 },
        { ...mockProduct, id: 3, quantity: 0, minQuantity: 10, unitPrice: 15 }
      ];
      component.computeStats();
      expect(component.stats.total).toBe(3);
      expect(component.stats.totalQty).toBe(25);
      expect(component.stats.totalValue).toBe(450);
      expect(component.stats.lowStock).toBe(1);
      expect(component.stats.outOfStock).toBe(1);
    });

    it('should handle empty products', () => {
      component.products = [];
      component.computeStats();
      expect(component.stats.total).toBe(0);
      expect(component.stats.totalQty).toBe(0);
      expect(component.stats.totalValue).toBe(0);
    });
  });

  describe('statusLabel', () => {
    it('should return correct labels', () => {
      expect(component.statusLabel('OK')).toBe('En stock');
      expect(component.statusLabel('LOW')).toBe('Stock bas');
      expect(component.statusLabel('OUT_OF_STOCK')).toBe('Rupture');
      expect(component.statusLabel('UNKNOWN')).toBe('UNKNOWN');
    });
  });

  describe('movementTypeLabel', () => {
    it('should return correct labels', () => {
      expect(component.movementTypeLabel('IN')).toBe('Entree');
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
    it('should open and close movement modal', () => {
      component.openMovement(mockProduct, 'IN');
      expect(component.showMovementModal).toBeTrue();
      expect(component.selectedProduct).toBe(mockProduct);
      expect(component.movementType).toBe('IN');
      expect(component.movementQty).toBe(0);
      component.closeMovement();
      expect(component.showMovementModal).toBeFalse();
      expect(component.selectedProduct).toBeNull();
    });
  });

  describe('submitMovement', () => {
    it('should not submit when no product selected', () => {
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
      component.movementQty = -5;
      component.submitMovement();
      expect(stockService.createStockMovement).not.toHaveBeenCalled();
    });

    it('should submit valid movement', () => {
      stockService.createStockMovement.and.returnValue(of({} as any));
      component.selectedProduct = mockProduct;
      component.movementType = 'IN';
      component.movementQty = 10;
      component.movementRef = 'REF-001';
      component.movementNotes = 'notes';
      component.submitMovement();
      expect(toast.success).toHaveBeenCalledWith('Mouvement enregistre');
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
      component.closeHistory();
      expect(component.showHistoryModal).toBeFalse();
      expect(component.historyProduct).toBeNull();
    });

    it('should handle history load error', () => {
      stockService.getStockMovements.and.returnValue(throwError(() => new Error('fail')));
      component.openHistory(mockProduct);
      expect(component.historyMovements.length).toBe(0);
      expect(component.loadingHistory).toBeFalse();
    });
  });
});
