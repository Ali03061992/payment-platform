import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';
import { SupplierManagementComponent } from './supplier-management.component';
import { OrganizationService } from '../../services/organization.service';
import { ToastService } from '../../services/toast.service';

describe('SupplierManagementComponent', () => {
  let component: SupplierManagementComponent;
  let fixture: ComponentFixture<SupplierManagementComponent>;
  let orgService: jasmine.SpyObj<OrganizationService>;
  let toast: jasmine.SpyObj<ToastService>;

  beforeEach(() => {
    const orgSpy = jasmine.createSpyObj('OrganizationService', ['listSuppliers', 'createSupplier', 'activate', 'disable']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    orgSpy.listSuppliers.and.returnValue(of([]));
    orgSpy.createSupplier.and.returnValue(of({ id: 1, name: 'Sup1' } as any));
    orgSpy.activate.and.returnValue(of({} as any));
    orgSpy.disable.and.returnValue(of({} as any));

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [SupplierManagementComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: OrganizationService, useValue: orgSpy },
        { provide: ToastService, useValue: toastSpy }
      ]
    });
    fixture = TestBed.createComponent(SupplierManagementComponent);
    component = fixture.componentInstance;
    orgService = TestBed.inject(OrganizationService) as jasmine.SpyObj<OrganizationService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load suppliers on init', () => {
    orgService.listSuppliers.and.returnValue(of([{ id: 1, name: 'S1', status: 'ACTIVE' } as any]));
    component.ngOnInit();
    expect(component.suppliers.length).toBe(1);
    expect(component.loading).toBeFalse();
  });

  it('should handle load error', () => {
    orgService.listSuppliers.and.returnValue(throwError(() => new Error('fail')));
    component.load();
    expect(component.loading).toBeFalse();
  });

  it('should create supplier with valid name', () => {
    component.newName = 'New Supplier';
    component.create();
    expect(orgService.createSupplier).toHaveBeenCalledWith('New Supplier');
    expect(toast.success).toHaveBeenCalled();
    expect(component.newName).toBe('');
    expect(component.showCreate).toBeFalse();
  });

  it('should not create supplier with empty name', () => {
    component.newName = '';
    component.create();
    expect(orgService.createSupplier).not.toHaveBeenCalled();
  });

  it('should not create supplier with whitespace name', () => {
    component.newName = '   ';
    component.create();
    expect(orgService.createSupplier).not.toHaveBeenCalled();
  });

  it('should handle create error with message', () => {
    component.newName = 'Supplier';
    orgService.createSupplier.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
    component.create();
    expect(toast.error).toHaveBeenCalledWith('Err');
    expect(component.creating).toBeFalse();
  });

  it('should handle create error without message', () => {
    component.newName = 'Supplier';
    orgService.createSupplier.and.returnValue(throwError(() => ({})));
    component.create();
    expect(toast.error).toHaveBeenCalledWith('Erreur lors de la création');
  });

  it('should disable active supplier', () => {
    component.toggle({ id: 1, status: 'ACTIVE' } as any);
    expect(orgService.disable).toHaveBeenCalledWith(1, 'SUPPLIER');
  });

  it('should activate inactive supplier', () => {
    component.toggle({ id: 1, status: 'INACTIVE' } as any);
    expect(orgService.activate).toHaveBeenCalledWith(1, 'SUPPLIER');
  });

  it('should handle toggle error silently', () => {
    orgService.disable.and.returnValue(throwError(() => new Error('fail')));
    component.toggle({ id: 1, status: 'ACTIVE' } as any);
    expect(orgService.disable).toHaveBeenCalled();
  });
});
