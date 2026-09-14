import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';
import { RelationManagementComponent } from './relation-management.component';
import { OrganizationService } from '../../services/organization.service';
import { ToastService } from '../../services/toast.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('RelationManagementComponent', () => {
  let component: RelationManagementComponent;
  let fixture: ComponentFixture<RelationManagementComponent>;
  let orgService: jasmine.SpyObj<OrganizationService>;
  let toast: jasmine.SpyObj<ToastService>;

  beforeEach(() => {
    const orgSpy = jasmine.createSpyObj('OrganizationService', ['listRelations', 'listSuppliers', 'listShops', 'createRelation', 'deactivateRelation']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    orgSpy.listRelations.and.returnValue(of([]));
    orgSpy.listSuppliers.and.returnValue(of([{ id: 1, name: 'Sup1' } as any]));
    orgSpy.listShops.and.returnValue(of([{ id: 2, name: 'Shop1' } as any]));
    orgSpy.createRelation.and.returnValue(of({} as any));
    orgSpy.deactivateRelation.and.returnValue(of(undefined));

    TestBed.configureTestingModule({
    declarations: [RelationManagementComponent],
    schemas: [NO_ERRORS_SCHEMA],
    imports: [],
    providers: [
        { provide: OrganizationService, useValue: orgSpy },
        { provide: ToastService, useValue: toastSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(RelationManagementComponent);
    component = fixture.componentInstance;
    orgService = TestBed.inject(OrganizationService) as jasmine.SpyObj<OrganizationService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load data on init', () => {
    component.ngOnInit();
    expect(component.relations.length).toBe(0);
    expect(component.suppliers.length).toBe(1);
    expect(component.shops.length).toBe(1);
    expect(component.loading).toBeFalse();
  });

  it('should create relation with valid selection', () => {
    component.selectedSupplierId = 1;
    component.selectedShopId = 2;
    component.create();
    expect(orgService.createRelation).toHaveBeenCalledWith({ supplierId: 1, shopId: 2 });
    expect(toast.success).toHaveBeenCalled();
    expect(component.showCreate).toBeFalse();
  });

  it('should not create relation without supplier', () => {
    component.selectedSupplierId = '';
    component.selectedShopId = 2;
    component.create();
    expect(orgService.createRelation).not.toHaveBeenCalled();
  });

  it('should not create relation without shop', () => {
    component.selectedSupplierId = 1;
    component.selectedShopId = '';
    component.create();
    expect(orgService.createRelation).not.toHaveBeenCalled();
  });

  it('should handle create error', () => {
    component.selectedSupplierId = 1;
    component.selectedShopId = 2;
    orgService.createRelation.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
    component.create();
    expect(toast.error).toHaveBeenCalledWith('Err');
    expect(component.creating).toBeFalse();
  });

  it('should handle create error without message', () => {
    component.selectedSupplierId = 1;
    component.selectedShopId = 2;
    orgService.createRelation.and.returnValue(throwError(() => ({})));
    component.create();
    expect(toast.error).toHaveBeenCalledWith('Erreur lors de la création');
  });

  it('should deactivate relation', () => {
    component.deactivate(5);
    expect(orgService.deactivateRelation).toHaveBeenCalledWith(5);
  });

  it('should get supplier name by id', () => {
    component.suppliers = [{ id: 1, name: 'Sup1' } as any];
    expect(component.getSupplierName(1)).toBe('Sup1');
    expect(component.getSupplierName(99)).toBe('#99');
  });

  it('should get shop name by id', () => {
    component.shops = [{ id: 2, name: 'Shop1' } as any];
    expect(component.getShopName(2)).toBe('Shop1');
    expect(component.getShopName(99)).toBe('#99');
  });
});
