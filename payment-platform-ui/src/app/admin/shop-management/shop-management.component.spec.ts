import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';
import { ShopManagementComponent } from './shop-management.component';
import { OrganizationService } from '../../services/organization.service';
import { ToastService } from '../../services/toast.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('ShopManagementComponent', () => {
  let component: ShopManagementComponent;
  let fixture: ComponentFixture<ShopManagementComponent>;
  let orgService: jasmine.SpyObj<OrganizationService>;
  let toast: jasmine.SpyObj<ToastService>;

  beforeEach(() => {
    const orgSpy = jasmine.createSpyObj('OrganizationService', ['listShops', 'createShop', 'activate', 'disable']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    orgSpy.listShops.and.returnValue(of([]));
    orgSpy.createShop.and.returnValue(of({ id: 1, name: 'Shop1' } as any));
    orgSpy.activate.and.returnValue(of({} as any));
    orgSpy.disable.and.returnValue(of({} as any));

    TestBed.configureTestingModule({
    declarations: [ShopManagementComponent],
    schemas: [NO_ERRORS_SCHEMA],
    imports: [],
    providers: [
        { provide: OrganizationService, useValue: orgSpy },
        { provide: ToastService, useValue: toastSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(ShopManagementComponent);
    component = fixture.componentInstance;
    orgService = TestBed.inject(OrganizationService) as jasmine.SpyObj<OrganizationService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load shops on init', () => {
    orgService.listShops.and.returnValue(of([{ id: 1, name: 'S1', status: 'ACTIVE' } as any]));
    component.ngOnInit();
    expect(component.shops.length).toBe(1);
    expect(component.loading).toBeFalse();
  });

  it('should handle load error', () => {
    orgService.listShops.and.returnValue(throwError(() => new Error('fail')));
    component.load();
    expect(component.loading).toBeFalse();
  });

  it('should create shop with valid name', () => {
    component.newName = 'New Shop';
    component.create();
    expect(orgService.createShop).toHaveBeenCalledWith('New Shop');
    expect(toast.success).toHaveBeenCalled();
    expect(component.newName).toBe('');
    expect(component.showCreate).toBeFalse();
  });

  it('should not create shop with empty name', () => {
    component.newName = '';
    component.create();
    expect(orgService.createShop).not.toHaveBeenCalled();
  });

  it('should not create shop with whitespace-only name', () => {
    component.newName = '   ';
    component.create();
    expect(orgService.createShop).not.toHaveBeenCalled();
  });

  it('should handle create error', () => {
    component.newName = 'Shop';
    orgService.createShop.and.returnValue(throwError(() => ({ error: { message: 'Erreur' } })));
    component.create();
    expect(toast.error).toHaveBeenCalledWith('Erreur');
    expect(component.creating).toBeFalse();
  });

  it('should handle create error with fallback message', () => {
    component.newName = 'Shop';
    orgService.createShop.and.returnValue(throwError(() => ({})));
    component.create();
    expect(toast.error).toHaveBeenCalledWith('Erreur lors de la création');
  });

  it('should toggle disable active shop', () => {
    component.toggle({ id: 1, status: 'ACTIVE' } as any);
    expect(orgService.disable).toHaveBeenCalledWith(1, 'SHOP');
  });

  it('should toggle activate inactive shop', () => {
    component.toggle({ id: 1, status: 'INACTIVE' } as any);
    expect(orgService.activate).toHaveBeenCalledWith(1, 'SHOP');
  });
});
