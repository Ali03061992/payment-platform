// @ts-nocheck
/**
 * Tests du service StockService.
 * Perimetre : instanciation et blocs getProducts, getProduct, createProduct, updateProduct, deleteProduct (voir blocs describe/it).
 * Moyens : HttpTestingController, TestBed, client HTTP de test.
 */
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { StockService } from './stock.service';
import { Product } from '../models/stock.model';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

const mockProduct: Product = {
  id: 1, supplierId: 1, name: 'Product A', sku: 'SKU-001', description: 'Desc',
  unitPrice: 10, currency: 'TND', quantity: 100, minQuantity: 10, reservedQty: 0,
  categoryId: null, familyId: null, unit: 'unite', status: 'ACTIVE', createdAt: '', updatedAt: ''
};

describe('StockService', () => {
  let service: StockService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    sessionStorage.setItem('user', JSON.stringify({ organizationId: 1 }));
    TestBed.configureTestingModule({
    imports: [],
    providers: [StockService, provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
});
    service = TestBed.inject(StockService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getProducts', () => {
    it('should GET products without status', () => {
      service.getProducts().subscribe(data => {
        expect(data.length).toBe(1);
      });
      const req = httpMock.expectOne(r => r.url === '/api/suppliers/1/products');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.has('status')).toBeFalse();
      req.flush([mockProduct]);
    });

    it('should GET products with status', () => {
      service.getProducts('ACTIVE').subscribe();
      const req = httpMock.expectOne(r => r.url === '/api/suppliers/1/products');
      expect(req.request.params.get('status')).toBe('ACTIVE');
      req.flush([mockProduct]);
    });
  });

  describe('getProduct', () => {
    it('should GET product by id', () => {
      service.getProduct(1).subscribe(data => {
        expect(data.id).toBe(1);
      });
      const req = httpMock.expectOne('/api/suppliers/1/products/1');
      expect(req.request.method).toBe('GET');
      req.flush(mockProduct);
    });
  });

  describe('createProduct', () => {
    it('should POST to create product', () => {
      service.createProduct({ name: 'P', sku: 'S', description: '', unitPrice: 10, currency: 'TND', quantity: 5, minQuantity: 0 }).subscribe(data => {
        expect(data.id).toBe(1);
      });
      const req = httpMock.expectOne('/api/suppliers/1/products');
      expect(req.request.method).toBe('POST');
      req.flush(mockProduct);
    });
  });

  describe('updateProduct', () => {
    it('should PATCH to update product', () => {
      service.updateProduct(1, { name: 'Updated' }).subscribe(data => {
        expect(data.name).toBe('Product A');
      });
      const req = httpMock.expectOne('/api/suppliers/1/products/1');
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ name: 'Updated' });
      req.flush(mockProduct);
    });
  });

  describe('deleteProduct', () => {
    it('should PATCH to deactivate product', () => {
      service.deleteProduct(1).subscribe();
      const req = httpMock.expectOne('/api/suppliers/1/products/1/deactivate');
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ status: 'INACTIVE' });
      req.flush(mockProduct);
    });
  });

  describe('getStockMovements', () => {
    it('should GET movements without productId', () => {
      service.getStockMovements().subscribe(data => {
        expect(data.length).toBe(0);
      });
      const req = httpMock.expectOne(r => r.url === '/api/suppliers/1/movements');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.has('productId')).toBeFalse();
      req.flush([]);
    });

    it('should GET movements with productId', () => {
      service.getStockMovements(5).subscribe();
      const req = httpMock.expectOne(r => r.url === '/api/suppliers/1/movements');
      expect(req.request.params.get('productId')).toBe('5');
      req.flush([]);
    });
  });

  describe('createStockMovement', () => {
    it('should POST to create stock movement', () => {
      const movementReq = { type: 'IN' as const, quantity: 10, reference: 'REF', notes: 'Restock' };
      service.createStockMovement(1, movementReq).subscribe();
      const req = httpMock.expectOne('/api/suppliers/1/movements');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ ...movementReq, productId: 1 });
      req.flush({ id: 1, ...movementReq, productId: 1 });
    });
  });

  describe('getSupplierId', () => {
    it('should return empty string when no user in session', () => {
      sessionStorage.clear();
      service.getProducts().subscribe();
      const req = httpMock.expectOne(r => r.url === '/api/suppliers//products');
      req.flush([]);
    });
  });
});
