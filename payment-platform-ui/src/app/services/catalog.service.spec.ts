// @ts-nocheck
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CatalogService } from './catalog.service';
import { ProductCategory, ProductFamily } from '../models/catalog.model';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('CatalogService', () => {
  let service: CatalogService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
    imports: [],
    providers: [CatalogService, provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
});
    service = TestBed.inject(CatalogService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('listCategories', () => {
    it('should GET categories with supplierId', () => {
      const mock: ProductCategory[] = [{ id: 1, supplierId: 1, name: 'Cat A', code: 'CA', status: 'ACTIVE', createdAt: '', updatedAt: '' }];
      service.listCategories(1).subscribe(data => {
        expect(data.length).toBe(1);
      });
      const req = httpMock.expectOne(r => r.url === '/api/supplier/catalog/categories');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('supplierId')).toBe('1');
      req.flush(mock);
    });
  });

  describe('createCategory', () => {
    it('should POST category', () => {
      const mock: ProductCategory = { id: 1, supplierId: 1, name: 'New', code: 'NW', status: 'ACTIVE', createdAt: '', updatedAt: '' };
      service.createCategory({ supplierId: 1, name: 'New', code: 'NW' }).subscribe(data => {
        expect(data.name).toBe('New');
      });
      const req = httpMock.expectOne('/api/supplier/catalog/categories');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ supplierId: 1, name: 'New', code: 'NW' });
      req.flush(mock);
    });
  });

  describe('deleteCategory', () => {
    it('should DELETE category', () => {
      service.deleteCategory(1).subscribe();
      const req = httpMock.expectOne('/api/supplier/catalog/categories/1');
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('listFamilies', () => {
    it('should GET families with supplierId only', () => {
      service.listFamilies(1).subscribe();
      const req = httpMock.expectOne(r => r.url === '/api/supplier/catalog/families');
      expect(req.request.params.get('supplierId')).toBe('1');
      expect(req.request.params.has('categoryId')).toBeFalse();
      req.flush([]);
    });

    it('should GET families with categoryId', () => {
      service.listFamilies(1, 5).subscribe();
      const req = httpMock.expectOne(r => r.url === '/api/supplier/catalog/families');
      expect(req.request.params.get('supplierId')).toBe('1');
      expect(req.request.params.get('categoryId')).toBe('5');
      req.flush([]);
    });
  });

  describe('createFamily', () => {
    it('should POST family', () => {
      const data = { supplierId: 1, name: 'Fam', code: 'FM', categoryIds: [1, 2] };
      const mock: ProductFamily = { id: 1, ...data, status: 'ACTIVE', categories: [], createdAt: '', updatedAt: '' };
      service.createFamily(data).subscribe(data => {
        expect(data.name).toBe('Fam');
      });
      const req = httpMock.expectOne('/api/supplier/catalog/families');
      expect(req.request.method).toBe('POST');
      req.flush(mock);
    });
  });

  describe('updateFamily', () => {
    it('should PUT family', () => {
      const data = { supplierId: 1, name: 'Updated', code: 'UP', categoryIds: [1] };
      service.updateFamily(1, data).subscribe();
      const req = httpMock.expectOne('/api/supplier/catalog/families/1');
      expect(req.request.method).toBe('PUT');
      req.flush({ id: 1, ...data });
    });
  });

  describe('deleteFamily', () => {
    it('should DELETE family', () => {
      service.deleteFamily(1).subscribe();
      const req = httpMock.expectOne('/api/supplier/catalog/families/1');
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });
});
