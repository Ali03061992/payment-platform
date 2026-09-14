import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { CreateUserComponent } from './create-user.component';
import { UserService } from '../../services/user.service';
import { OrganizationService } from '../../services/organization.service';
import { ToastService } from '../../services/toast.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('CreateUserComponent', () => {
  let component: CreateUserComponent;
  let fixture: ComponentFixture<CreateUserComponent>;
  let userService: jasmine.SpyObj<UserService>;
  let orgService: jasmine.SpyObj<OrganizationService>;
  let toast: jasmine.SpyObj<ToastService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    const userSpy = jasmine.createSpyObj('UserService', ['create']);
    const orgSpy = jasmine.createSpyObj('OrganizationService', ['listSuppliers', 'listShops']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    orgSpy.listSuppliers.and.returnValue(of([]));
    orgSpy.listShops.and.returnValue(of([]));

    TestBed.configureTestingModule({
    declarations: [CreateUserComponent],
    imports: [FormsModule],
    providers: [
        { provide: UserService, useValue: userSpy },
        { provide: OrganizationService, useValue: orgSpy },
        { provide: ToastService, useValue: toastSpy },
        { provide: Router, useValue: routerSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(CreateUserComponent);
    component = fixture.componentInstance;
    userService = TestBed.inject(UserService) as jasmine.SpyObj<UserService>;
    orgService = TestBed.inject(OrganizationService) as jasmine.SpyObj<OrganizationService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should load suppliers and shops', () => {
      orgService.listSuppliers.and.returnValue(of([{ id: 1, name: 'Sup' } as any]));
      orgService.listShops.and.returnValue(of([{ id: 2, name: 'Shop' } as any]));
      component.ngOnInit();
      expect(orgService.listSuppliers).toHaveBeenCalled();
      expect(orgService.listShops).toHaveBeenCalled();
    });
  });

  describe('requiresOrganization', () => {
    it('should return true for non-SYSTEM_ADMIN role', () => {
      component.form.role = 'SHOP_AGENT';
      expect(component.requiresOrganization).toBeTrue();
    });

    it('should return false for SYSTEM_ADMIN', () => {
      component.form.role = 'SYSTEM_ADMIN';
      expect(component.requiresOrganization).toBeFalse();
    });
  });

  describe('organizationLabel', () => {
    it('should return Fournisseur for SUPPLIER roles', () => {
      component.form.role = 'SUPPLIER_ADMIN';
      expect(component.organizationLabel).toBe('Fournisseur');
    });

    it('should return Boutique for SHOP roles', () => {
      component.form.role = 'SHOP_ADMIN';
      expect(component.organizationLabel).toBe('Boutique');
    });
  });

  describe('availableOrgs', () => {
    it('should return suppliers for SUPPLIER role', () => {
      component.suppliers = [{ id: 1, name: 'Sup' } as any];
      component.form.role = 'SUPPLIER_ADMIN';
      expect(component.availableOrgs.length).toBe(1);
    });

    it('should return shops for SHOP role', () => {
      component.shops = [{ id: 2, name: 'Shop' } as any];
      component.form.role = 'SHOP_ADMIN';
      expect(component.availableOrgs.length).toBe(1);
    });
  });

  describe('onSubmit', () => {
    it('should create user and show success', () => {
      userService.create.and.returnValue(of({} as any));
      component.onSubmit();
      expect(component.success).toBeTrue();
      expect(toast.success).toHaveBeenCalled();
    });

    it('should handle error', () => {
      userService.create.and.returnValue(throwError(() => ({ error: { message: 'Duplicate username' } })));
      component.onSubmit();
      expect(toast.error).toHaveBeenCalledWith('Duplicate username');
    });

    it('should default error message on failure', () => {
      userService.create.and.returnValue(throwError(() => ({ error: {} })));
      component.onSubmit();
      expect(toast.error).toHaveBeenCalledWith("Erreur lors de la création");
    });
  });
});
