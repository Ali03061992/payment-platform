import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { OrganizationService } from './organization.service';
import { Organization, SupplierShopRelation } from '../models/organization.model';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('OrganizationService', () => {
  let service: OrganizationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
    imports: [],
    providers: [OrganizationService, provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
});
    service = TestBed.inject(OrganizationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('listSuppliers', () => {
    it('should GET suppliers', () => {
      const mock: Organization[] = [
        { id: 1, name: 'Supplier A', type: 'SUPPLIER', status: 'ACTIVE', version: 1, createdAt: '', updatedAt: '', relations: [] }
      ];
      service.listSuppliers().subscribe(data => {
        expect(data.length).toBe(1);
        expect(data[0].name).toBe('Supplier A');
      });
      const req = httpMock.expectOne('/api/admin/suppliers');
      expect(req.request.method).toBe('GET');
      req.flush(mock);
    });
  });

  describe('listShops', () => {
    it('should GET shops', () => {
      const mock: Organization[] = [
        { id: 2, name: 'Shop B', type: 'SHOP', status: 'ACTIVE', version: 1, createdAt: '', updatedAt: '', relations: [] }
      ];
      service.listShops().subscribe(data => {
        expect(data.length).toBe(1);
        expect(data[0].type).toBe('SHOP');
      });
      const req = httpMock.expectOne('/api/admin/shops');
      expect(req.request.method).toBe('GET');
      req.flush(mock);
    });
  });

  describe('createSupplier', () => {
    it('should POST to create supplier', () => {
      const mock: Organization = { id: 3, name: 'New Supplier', type: 'SUPPLIER', status: 'ACTIVE', version: 1, createdAt: '', updatedAt: '', relations: [] };
      service.createSupplier('New Supplier').subscribe(data => {
        expect(data.name).toBe('New Supplier');
      });
      const req = httpMock.expectOne('/api/admin/suppliers');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ name: 'New Supplier' });
      req.flush(mock);
    });
  });

  describe('createShop', () => {
    it('should POST to create shop', () => {
      const mock: Organization = { id: 4, name: 'New Shop', type: 'SHOP', status: 'ACTIVE', version: 1, createdAt: '', updatedAt: '', relations: [] };
      service.createShop('New Shop').subscribe(data => {
        expect(data.name).toBe('New Shop');
      });
      const req = httpMock.expectOne('/api/admin/shops');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ name: 'New Shop' });
      req.flush(mock);
    });
  });

  describe('activate', () => {
    it('should PATCH activate supplier', () => {
      const mock: Organization = { id: 1, name: 'S', type: 'SUPPLIER', status: 'ACTIVE', version: 2, createdAt: '', updatedAt: '', relations: [] };
      service.activate(1, 'SUPPLIER').subscribe(data => {
        expect(data.status).toBe('ACTIVE');
      });
      const req = httpMock.expectOne('/api/admin/suppliers/1/activate');
      expect(req.request.method).toBe('PATCH');
      req.flush(mock);
    });

    it('should PATCH activate shop', () => {
      const mock: Organization = { id: 2, name: 'S', type: 'SHOP', status: 'ACTIVE', version: 2, createdAt: '', updatedAt: '', relations: [] };
      service.activate(2, 'SHOP').subscribe(data => {
        expect(data.status).toBe('ACTIVE');
      });
      const req = httpMock.expectOne('/api/admin/shops/2/activate');
      expect(req.request.method).toBe('PATCH');
      req.flush(mock);
    });
  });

  describe('disable', () => {
    it('should PATCH disable supplier', () => {
      const mock: Organization = { id: 1, name: 'S', type: 'SUPPLIER', status: 'DISABLED', version: 2, createdAt: '', updatedAt: '', relations: [] };
      service.disable(1, 'SUPPLIER').subscribe(data => {
        expect(data.status).toBe('DISABLED');
      });
      const req = httpMock.expectOne('/api/admin/suppliers/1/disable');
      expect(req.request.method).toBe('PATCH');
      req.flush(mock);
    });
  });

  describe('getStats', () => {
    it('should GET stats', () => {
      service.getStats().subscribe(data => {
        expect(data.suppliers).toBe(5);
        expect(data.shops).toBe(10);
      });
      const req = httpMock.expectOne('/api/admin/stats');
      expect(req.request.method).toBe('GET');
      req.flush({ suppliers: 5, shops: 10 });
    });
  });

  describe('listRelations', () => {
    it('should GET relations', () => {
      const mock: SupplierShopRelation[] = [
        { id: 1, supplierId: 1, shopId: 2, status: 'ACTIVE', createdAt: '' }
      ];
      service.listRelations().subscribe(data => {
        expect(data.length).toBe(1);
      });
      const req = httpMock.expectOne('/api/admin/supplier-shop-relations');
      expect(req.request.method).toBe('GET');
      req.flush(mock);
    });
  });

  describe('createRelation', () => {
    it('should POST to create relation', () => {
      const mock: SupplierShopRelation = { id: 1, supplierId: 1, shopId: 2, status: 'ACTIVE', createdAt: '' };
      service.createRelation({ supplierId: 1, shopId: 2 }).subscribe(data => {
        expect(data.supplierId).toBe(1);
        expect(data.shopId).toBe(2);
      });
      const req = httpMock.expectOne('/api/admin/supplier-shop-relations');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ supplierId: 1, shopId: 2 });
      req.flush(mock);
    });
  });

  describe('deactivateRelation', () => {
    it('should DELETE relation', () => {
      service.deactivateRelation(5).subscribe();
      const req = httpMock.expectOne('/api/admin/supplier-shop-relations/5');
      expect(req.request.method).toBe('DELETE');
      req.flush(null);
    });
  });

  describe('listRelationsByShop', () => {
    it('should GET relations by shop', () => {
      const mock: SupplierShopRelation[] = [
        { id: 1, supplierId: 1, shopId: 2, status: 'ACTIVE', createdAt: '' }
      ];
      service.listRelationsByShop(2).subscribe(data => {
        expect(data.length).toBe(1);
      });
      const req = httpMock.expectOne('/api/admin/supplier-shop-relations/shop/2');
      expect(req.request.method).toBe('GET');
      req.flush(mock);
    });
  });

  describe('listRelationsBySupplier', () => {
    it('should GET relations by supplier', () => {
      const mock: SupplierShopRelation[] = [
        { id: 1, supplierId: 1, shopId: 2, status: 'ACTIVE', createdAt: '' }
      ];
      service.listRelationsBySupplier(1).subscribe(data => {
        expect(data.length).toBe(1);
      });
      const req = httpMock.expectOne('/api/admin/supplier-shop-relations/supplier/1');
      expect(req.request.method).toBe('GET');
      req.flush(mock);
    });
  });

  describe('listUsers', () => {
    it('should GET users', () => {
      service.listUsers().subscribe(data => {
        expect(data.length).toBe(0);
      });
      const req = httpMock.expectOne('/api/users');
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });
  });

  describe('getById', () => {
    it('should GET organization by id', () => {
      const mock: Organization = { id: 1, name: 'Supplier A', type: 'SUPPLIER', status: 'ACTIVE', version: 1, createdAt: '', updatedAt: '', relations: [] };
      service.getById(1).subscribe(data => {
        expect(data.id).toBe(1);
      });
      const req = httpMock.expectOne('/api/admin/suppliers/1');
      expect(req.request.method).toBe('GET');
      req.flush(mock);
    });
  });

  describe('disable', () => {
    it('should PATCH disable shop', () => {
      const mock: Organization = { id: 2, name: 'S', type: 'SHOP', status: 'DISABLED', version: 2, createdAt: '', updatedAt: '', relations: [] };
      service.disable(2, 'SHOP').subscribe(data => {
        expect(data.status).toBe('DISABLED');
      });
      const req = httpMock.expectOne('/api/admin/shops/2/disable');
      expect(req.request.method).toBe('PATCH');
      req.flush(mock);
    });
  });
});
