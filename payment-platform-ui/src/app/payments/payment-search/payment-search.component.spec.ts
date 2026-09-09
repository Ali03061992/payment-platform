import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { PaymentSearchComponent } from './payment-search.component';
import { PaymentSearchService } from '../../services/payment-search.service';
import { OrganizationService } from '../../services/organization.service';
import { ToastService } from '../../services/toast.service';

describe('PaymentSearchComponent', () => {
  let component: PaymentSearchComponent;
  let fixture: ComponentFixture<PaymentSearchComponent>;
  let searchService: jasmine.SpyObj<PaymentSearchService>;
  let orgService: jasmine.SpyObj<OrganizationService>;
  let toast: jasmine.SpyObj<ToastService>;

  beforeEach(() => {
    sessionStorage.setItem('user', JSON.stringify({ roles: ['SYSTEM_ADMIN'], organizationId: 1 }));
    const searchSpy = jasmine.createSpyObj('PaymentSearchService', ['search']);
    const orgSpy = jasmine.createSpyObj('OrganizationService', ['listShops', 'listSuppliers', 'listRelationsBySupplier', 'getById']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    searchSpy.search.and.returnValue(of({ payments: [], total: 0, page: 0, size: 20 }));
    orgSpy.listShops.and.returnValue(of([]));
    orgSpy.listSuppliers.and.returnValue(of([]));
    orgSpy.listRelationsBySupplier.and.returnValue(of([]));
    orgSpy.getById.and.returnValue(of({ id: 10, name: 'Shop10' } as any));

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, FormsModule],
      declarations: [PaymentSearchComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: PaymentSearchService, useValue: searchSpy },
        { provide: OrganizationService, useValue: orgSpy },
        { provide: ToastService, useValue: toastSpy }
      ]
    });
    fixture = TestBed.createComponent(PaymentSearchComponent);
    component = fixture.componentInstance;
    searchService = TestBed.inject(PaymentSearchService) as jasmine.SpyObj<PaymentSearchService>;
    orgService = TestBed.inject(OrganizationService) as jasmine.SpyObj<OrganizationService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  afterEach(() => sessionStorage.clear());

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should load orgs for SYSTEM_ADMIN', () => {
      component.ngOnInit();
      expect(orgService.listShops).toHaveBeenCalled();
      expect(orgService.listSuppliers).toHaveBeenCalled();
    });

    it('should load shops via relations for non SYSTEM_ADMIN', () => {
      sessionStorage.setItem('user', JSON.stringify({ roles: ['SUPPLIER_ADMIN'], organizationId: 5 }));
      const comp = new PaymentSearchComponent(searchService as any, orgService as any, toast as any);
      orgService.listRelationsBySupplier.and.returnValue(of([
        { supplierId: 5, shopId: 10, status: 'ACTIVE' },
        { supplierId: 5, shopId: 20, status: 'INACTIVE' },
        { supplierId: 5, shopId: 30, status: 'ACTIVE' }
      ] as any));
      comp.ngOnInit();
      expect(orgService.listRelationsBySupplier).toHaveBeenCalledWith(5);
      expect(orgService.getById).toHaveBeenCalled();
    });

    it('should not load orgs when no user', () => {
      sessionStorage.clear();
      const comp = new PaymentSearchComponent(searchService as any, orgService as any, toast as any);
      comp.ngOnInit();
      expect(orgService.listShops).not.toHaveBeenCalled();
      expect(orgService.listSuppliers).not.toHaveBeenCalled();
      expect(orgService.listRelationsBySupplier).not.toHaveBeenCalled();
    });

    it('should not load orgs when user has no roles', () => {
      sessionStorage.setItem('user', JSON.stringify({ organizationId: 1 }));
      const comp = new PaymentSearchComponent(searchService as any, orgService as any, toast as any);
      comp.ngOnInit();
      expect(orgService.listShops).not.toHaveBeenCalled();
    });

    it('should not load relations when user has no organizationId', () => {
      sessionStorage.setItem('user', JSON.stringify({ roles: ['SUPPLIER_ADMIN'] }));
      const comp = new PaymentSearchComponent(searchService as any, orgService as any, toast as any);
      comp.ngOnInit();
      expect(orgService.listRelationsBySupplier).not.toHaveBeenCalled();
    });
  });

  describe('search', () => {
    it('should perform search', () => {
      searchService.search.and.returnValue(of({ payments: [{ id: 1 } as any], total: 1, page: 0, size: 20 }));
      component.search();
      expect(component.results.length).toBe(1);
      expect(component.total).toBe(1);
      expect(component.loading).toBeFalse();
    });

    it('should handle error with message', () => {
      searchService.search.and.returnValue(throwError(() => ({ error: { message: 'Fail' } })));
      component.search();
      expect(toast.error).toHaveBeenCalledWith('Fail');
      expect(component.loading).toBeFalse();
    });

    it('should handle error without message', () => {
      searchService.search.and.returnValue(throwError(() => ({})));
      component.search();
      expect(toast.error).toHaveBeenCalledWith('Erreur de recherche');
    });
  });

  describe('resetFilters', () => {
    it('should reset filters and search', () => {
      component.filters = { page: 5, size: 20, status: 'PENDING' };
      component.resetFilters();
      expect(component.filters.page).toBe(0);
      expect(component.filters.size).toBe(20);
      expect(component.filters.status).toBeUndefined();
    });
  });

  describe('nextPage / prevPage', () => {
    it('should increment page', () => {
      component.filters.page = 0;
      component.nextPage();
      expect(component.filters.page).toBe(1);
    });

    it('should decrement page', () => {
      component.filters.page = 2;
      component.prevPage();
      expect(component.filters.page).toBe(1);
    });

    it('should not go below 0', () => {
      component.filters.page = 0;
      component.prevPage();
      expect(component.filters.page).toBe(0);
    });
  });

  describe('statusLabel', () => {
    it('should return correct labels', () => {
      expect(component.statusLabel('PENDING')).toBe('En attente');
      expect(component.statusLabel('CONFIRMED')).toBe('Confirmé');
      expect(component.statusLabel('REJECTED')).toBe('Rejeté');
      expect(component.statusLabel('CANCELLED')).toBe('Annulé');
      expect(component.statusLabel('UNKNOWN')).toBe('UNKNOWN');
    });
  });

  describe('totalPages', () => {
    it('should compute total pages', () => {
      component.total = 50;
      component.filters.size = 20;
      expect(component.totalPages).toBe(3);
    });

    it('should return 0 for no results', () => {
      component.total = 0;
      expect(component.totalPages).toBe(0);
    });
  });
});
