// @ts-nocheck
/**
 * Tests du composant ProductManagementComponent.
 * Perimetre : instanciation et blocs ngOnInit, supplierId, filteredProducts, getCategoryName, getFamilyName (voir blocs describe/it).
 * Moyens : TestBed + fixture, stubs jasmine, client HTTP de test, observables RxJS mockes.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ProductManagementComponent } from './product-management.component';
import { StockService } from '../../services/stock.service';
import { CatalogService } from '../../services/catalog.service';
import { ToastService } from '../../services/toast.service';
import { Product } from '../../models/stock.model';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('ProductManagementComponent', () => {
  let component: ProductManagementComponent;
  let fixture: ComponentFixture<ProductManagementComponent>;
  let stockService: jasmine.SpyObj<StockService>;
  let catalogService: jasmine.SpyObj<CatalogService>;
  let toast: jasmine.SpyObj<ToastService>;

  const mockProduct = {
    id: 1, supplierId: 1, name: 'Product A', sku: 'SKU-001', description: 'desc',
    unitPrice: 10, currency: 'TND', quantity: 5, minQuantity: 1, unit: 'unite',
    categoryId: 1, familyId: 1, status: 'ACTIVE', createdAt: '', updatedAt: ''
  } as any;

  beforeEach(() => {
    sessionStorage.setItem('user', JSON.stringify({ organizationId: 1 }));
    const stockSpy = jasmine.createSpyObj('StockService', ['getProducts', 'createProduct', 'updateProduct', 'deleteProduct']);
    const catalogSpy = jasmine.createSpyObj('CatalogService', ['listCategories', 'listFamilies']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    stockSpy.getProducts.and.returnValue(of([]));
    catalogSpy.listCategories.and.returnValue(of([]));
    catalogSpy.listFamilies.and.returnValue(of([]));

    TestBed.configureTestingModule({
    declarations: [ProductManagementComponent],
    schemas: [NO_ERRORS_SCHEMA],
    imports: [FormsModule],
    providers: [
        { provide: StockService, useValue: stockSpy },
        { provide: CatalogService, useValue: catalogSpy },
        { provide: ToastService, useValue: toastSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(ProductManagementComponent);
    component = fixture.componentInstance;
    stockService = TestBed.inject(StockService) as jasmine.SpyObj<StockService>;
    catalogService = TestBed.inject(CatalogService) as jasmine.SpyObj<CatalogService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  afterEach(() => sessionStorage.clear());

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should load data', () => {
      stockService.getProducts.and.returnValue(of([mockProduct]));
      catalogService.listCategories.and.returnValue(of([{ id: 1, name: 'Cat1' } as any]));
      catalogService.listFamilies.and.returnValue(of([{ id: 1, name: 'Fam1' } as any]));
      component.ngOnInit();
      expect(component.products.length).toBe(1);
      expect(component.loading).toBeFalse();
    });

    it('should handle load products error', () => {
      stockService.getProducts.and.returnValue(throwError(() => new Error('fail')));
      component.ngOnInit();
      expect(component.products.length).toBe(0);
      expect(component.loading).toBeFalse();
    });

    it('should handle load categories error', () => {
      catalogService.listCategories.and.returnValue(throwError(() => new Error('fail')));
      component.ngOnInit();
      expect(component.categories.length).toBe(0);
    });

    it('should handle load families error', () => {
      catalogService.listFamilies.and.returnValue(throwError(() => new Error('fail')));
      component.ngOnInit();
      expect(component.families.length).toBe(0);
    });
  });

  describe('supplierId', () => {
    it('should return organizationId from session', () => {
      expect(component.supplierId).toBe(1);
    });

    it('should return empty string when no user', () => {
      sessionStorage.clear();
      expect(component.supplierId).toBe('');
    });
  });

  describe('filteredProducts', () => {
    it('should filter by category', () => {
      component.products = [
        { id: 1, categoryId: '1' } as any,
        { id: 2, categoryId: '2' } as any
      ];
      component.filterCategory = '1';
      expect(component.filteredProducts.length).toBe(1);
    });

    it('should filter by family', () => {
      component.products = [
        { id: 1, familyId: '1' } as any,
        { id: 2, familyId: '2' } as any
      ];
      component.filterFamily = '1';
      expect(component.filteredProducts.length).toBe(1);
    });

    it('should filter by both category and family', () => {
      component.products = [
        { id: 1, categoryId: '1', familyId: '1' } as any,
        { id: 2, categoryId: '1', familyId: '2' } as any,
        { id: 3, categoryId: '2', familyId: '1' } as any
      ];
      component.filterCategory = '1';
      component.filterFamily = '1';
      expect(component.filteredProducts.length).toBe(1);
    });

    it('should return all when no filter', () => {
      component.products = [{ id: 1 } as Product, { id: 2 } as Product];
      component.filterCategory = '';
      component.filterFamily = '';
      expect(component.filteredProducts.length).toBe(2);
    });
  });

  describe('getCategoryName', () => {
    it('should return category name', () => {
      component.categories = [{ id: 1, name: 'Cat A' } as any];
      expect(component.getCategoryName(1)).toBe('Cat A');
    });

    it('should return fallback for unknown id', () => {
      expect(component.getCategoryName(999)).toBe('-');
    });

    it('should return fallback for null', () => {
      expect(component.getCategoryName(null)).toBe('-');
    });
  });

  describe('getFamilyName', () => {
    it('should return family name', () => {
      component.families = [{ id: 1, name: 'Fam A' } as any];
      expect(component.getFamilyName(1)).toBe('Fam A');
    });

    it('should return fallback for unknown id', () => {
      expect(component.getFamilyName(999)).toBe('-');
    });

    it('should return fallback for null', () => {
      expect(component.getFamilyName(null)).toBe('-');
    });
  });

  describe('onCategoryChange', () => {
    it('should reset familyId', () => {
      component.form.familyId = 5;
      component.onCategoryChange();
      expect(component.form.familyId).toBe('');
    });
  });

  describe('getFamiliesForCategory', () => {
    it('should return all families when no category selected', () => {
      component.families = [{ id: 1 } as any, { id: 2 } as any];
      component.form.categoryId = '';
      expect(component.getFamiliesForCategory().length).toBe(2);
    });

    it('should filter families by category', () => {
      component.families = [
        { id: 1, categories: [{ id: 10 }] } as any,
        { id: 2, categories: [{ id: 20 }] } as any,
        { id: 3, categories: [{ id: 10 }, { id: 20 }] } as any
      ];
      component.form.categoryId = 10;
      const result = component.getFamiliesForCategory();
      expect(result.length).toBe(2);
    });

    it('should return empty when no families match', () => {
      component.families = [{ id: 1, categories: [{ id: 99 }] } as any];
      component.form.categoryId = 10;
      expect(component.getFamiliesForCategory().length).toBe(0);
    });
  });

  describe('openCreate', () => {
    it('should open create form', () => {
      component.openCreate();
      expect(component.showForm).toBeTrue();
      expect(component.editingProduct).toBeNull();
      expect(component.form.name).toBe('');
    });
  });

  describe('openEdit', () => {
    it('should open edit form with product data', () => {
      component.openEdit(mockProduct);
      expect(component.editingProduct).toBe(mockProduct);
      expect(component.form.name).toBe('Product A');
      expect(component.form.sku).toBe('SKU-001');
      expect(component.form.description).toBe('desc');
    });

    it('should handle product with no categoryId/familyId/unit', () => {
      const p = { ...mockProduct, categoryId: null, familyId: null, unit: null } as any;
      component.openEdit(p);
      expect(component.form.categoryId).toBe('');
      expect(component.form.familyId).toBe('');
      expect(component.form.unit).toBe('unite');
    });
  });

  describe('save', () => {
    it('should not save when name is empty', () => {
      component.form.name = '';
      component.form.sku = 'SKU';
      component.save();
      expect(stockService.createProduct).not.toHaveBeenCalled();
    });

    it('should not save when sku is empty', () => {
      component.form.name = 'Product';
      component.form.sku = '';
      component.save();
      expect(stockService.createProduct).not.toHaveBeenCalled();
    });

    it('should not save when name is whitespace', () => {
      component.form.name = '   ';
      component.form.sku = 'SKU';
      component.save();
      expect(stockService.createProduct).not.toHaveBeenCalled();
    });

    it('should create product when no editing', () => {
      stockService.createProduct.and.returnValue(of({} as any));
      component.form.name = 'New Product';
      component.form.sku = 'NEW-001';
      component.save();
      expect(stockService.createProduct).toHaveBeenCalled();
      expect(toast.success).toHaveBeenCalledWith('Produit cree');
    });

    it('should handle create error', () => {
      stockService.createProduct.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
      component.form.name = 'New Product';
      component.form.sku = 'NEW-001';
      component.save();
      expect(toast.error).toHaveBeenCalledWith('Err');
    });

    it('should handle create error without message', () => {
      stockService.createProduct.and.returnValue(throwError(() => ({})));
      component.form.name = 'New Product';
      component.form.sku = 'NEW-001';
      component.save();
      expect(toast.error).toHaveBeenCalledWith('Erreur');
    });

    it('should update product when editing', () => {
      stockService.updateProduct.and.returnValue(of({} as any));
      component.editingProduct = mockProduct;
      component.form.name = 'Updated';
      component.form.sku = 'UPD-001';
      component.save();
      expect(stockService.updateProduct).toHaveBeenCalledWith(1, jasmine.objectContaining({ name: 'Updated' }));
      expect(toast.success).toHaveBeenCalledWith('Produit mis a jour');
    });

    it('should handle update error', () => {
      stockService.updateProduct.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
      component.editingProduct = mockProduct;
      component.form.name = 'Updated';
      component.form.sku = 'UPD-001';
      component.save();
      expect(toast.error).toHaveBeenCalledWith('Err');
    });
  });

  describe('toggleStatus', () => {
    it('should toggle ACTIVE to INACTIVE', () => {
      stockService.updateProduct.and.returnValue(of({} as any));
      component.toggleStatus({ ...mockProduct, status: 'ACTIVE' } as any);
      expect(stockService.updateProduct).toHaveBeenCalledWith(1, { status: 'INACTIVE' });
    });

    it('should toggle INACTIVE to ACTIVE', () => {
      stockService.updateProduct.and.returnValue(of({} as any));
      component.toggleStatus({ ...mockProduct, status: 'INACTIVE' } as any);
      expect(stockService.updateProduct).toHaveBeenCalledWith(1, { status: 'ACTIVE' });
    });

    it('should handle toggle error', () => {
      stockService.updateProduct.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
      component.toggleStatus(mockProduct);
      expect(toast.error).toHaveBeenCalledWith('Err');
    });
  });

  describe('deleteProduct', () => {
    it('should delete on confirm', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      stockService.deleteProduct.and.returnValue(of({} as any));
      component.deleteProduct({ id: 1, name: 'P' } as any);
      expect(stockService.deleteProduct).toHaveBeenCalledWith(1);
      expect(toast.success).toHaveBeenCalledWith('Produit supprime');
    });

    it('should not delete when not confirmed', () => {
      spyOn(window, 'confirm').and.returnValue(false);
      component.deleteProduct({ id: 1, name: 'P' } as any);
      expect(stockService.deleteProduct).not.toHaveBeenCalled();
    });

    it('should handle delete error', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      stockService.deleteProduct.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
      component.deleteProduct({ id: 1, name: 'P' } as any);
      expect(toast.error).toHaveBeenCalledWith('Err');
    });
  });

  describe('statusLabel', () => {
    it('should return correct labels', () => {
      expect(component.statusLabel('ACTIVE')).toBe('Actif');
      expect(component.statusLabel('INACTIVE')).toBe('Inactif');
      expect(component.statusLabel('OUT_OF_STOCK')).toBe('Hors stock');
      expect(component.statusLabel('UNKNOWN')).toBe('UNKNOWN');
    });
  });
});
