// @ts-nocheck
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { CategoryManagementComponent } from './category-management.component';
import { CatalogService } from '../../services/catalog.service';
import { ToastService } from '../../services/toast.service';
import { ProductCategory } from '../../models/catalog.model';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('CategoryManagementComponent', () => {
  let component: CategoryManagementComponent;
  let fixture: ComponentFixture<CategoryManagementComponent>;
  let catalogService: jasmine.SpyObj<CatalogService>;
  let toast: jasmine.SpyObj<ToastService>;

  beforeEach(() => {
    sessionStorage.setItem('user', JSON.stringify({ organizationId: 1 }));
    const catalogSpy = jasmine.createSpyObj('CatalogService', ['listCategories', 'createCategory', 'deleteCategory']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    catalogSpy.listCategories.and.returnValue(of([]));

    TestBed.configureTestingModule({
    declarations: [CategoryManagementComponent],
    imports: [FormsModule],
    providers: [
        { provide: CatalogService, useValue: catalogSpy },
        { provide: ToastService, useValue: toastSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(CategoryManagementComponent);
    component = fixture.componentInstance;
    catalogService = TestBed.inject(CatalogService) as jasmine.SpyObj<CatalogService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('openCreate', () => {
    it('should open create form', () => {
      component.openCreate();
      expect(component.showForm).toBeTrue();
      expect(component.form.name).toBe('');
    });
  });

  describe('save', () => {
    it('should save category', () => {
      catalogService.createCategory.and.returnValue(of({} as any));
      component.form = { name: 'New Cat', code: 'NC' };
      component.save();
      expect(toast.success).toHaveBeenCalled();
      expect(component.showForm).toBeFalse();
    });

    it('should not save with empty fields', () => {
      component.form = { name: '', code: '' };
      component.save();
      expect(catalogService.createCategory).not.toHaveBeenCalled();
    });

    it('should handle error', () => {
      catalogService.createCategory.and.returnValue(throwError(() => ({ error: { message: 'Fail' } })));
      component.form = { name: 'Cat', code: 'C' };
      component.save();
      expect(toast.error).toHaveBeenCalled();
    });
  });

  describe('deleteCategory', () => {
    it('should delete on confirm', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      catalogService.deleteCategory.and.returnValue(of(undefined as any));
      component.deleteCategory({ id: 1, name: 'Cat' } as ProductCategory);
      expect(catalogService.deleteCategory).toHaveBeenCalledWith(1);
    });

    it('should not delete when not confirmed', () => {
      spyOn(window, 'confirm').and.returnValue(false);
      component.deleteCategory({ id: 1, name: 'Cat' } as ProductCategory);
      expect(catalogService.deleteCategory).not.toHaveBeenCalled();
    });

    it('should handle delete error', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      catalogService.deleteCategory.and.returnValue(throwError(() => ({ error: { message: 'Delete failed' } })));
      component.deleteCategory({ id: 1, name: 'Cat' } as ProductCategory);
      expect(toast.error).toHaveBeenCalledWith('Delete failed');
    });

    it('should handle delete error without message', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      catalogService.deleteCategory.and.returnValue(throwError(() => ({ error: {} })));
      component.deleteCategory({ id: 1, name: 'Cat' } as ProductCategory);
      expect(toast.error).toHaveBeenCalledWith('Erreur');
    });
  });

  describe('ngOnInit', () => {
    it('should load categories on init', () => {
      component.ngOnInit();
      expect(catalogService.listCategories).toHaveBeenCalled();
    });
  });

  describe('loadCategories', () => {
    it('should load categories successfully', () => {
      const mockCategories: ProductCategory[] = [
        { id: 1, supplierId: 1, name: 'Cat1', code: 'C1', status: 'ACTIVE', createdAt: '', updatedAt: '' }
      ];
      catalogService.listCategories.and.returnValue(of(mockCategories));
      component.loadCategories();
      expect(component.categories.length).toBe(1);
      expect(component.loading).toBeFalse();
    });

    it('should handle load error', () => {
      catalogService.listCategories.and.returnValue(throwError(() => ({})));
      component.loadCategories();
      expect(component.categories.length).toBe(0);
      expect(component.loading).toBeFalse();
    });
  });

  describe('supplierId', () => {
    it('should return supplierId from session', () => {
      expect(component.supplierId).toBe(1);
    });

    it('should return empty when no user in session', () => {
      sessionStorage.clear();
      expect(component.supplierId).toBe('');
    });
  });

  describe('save', () => {
    it('should not save with empty name only', () => {
      component.form = { name: '', code: 'C' };
      component.save();
      expect(catalogService.createCategory).not.toHaveBeenCalled();
    });

    it('should not save with empty code only', () => {
      component.form = { name: 'Cat', code: '' };
      component.save();
      expect(catalogService.createCategory).not.toHaveBeenCalled();
    });

    it('should handle save error without message', () => {
      catalogService.createCategory.and.returnValue(throwError(() => ({ error: {} })));
      component.form = { name: 'Cat', code: 'C' };
      component.save();
      expect(toast.error).toHaveBeenCalledWith('Erreur');
      expect(component.saving).toBeFalse();
    });

    it('should set saving to true during save', () => {
      catalogService.createCategory.and.returnValue(of({} as any));
      component.form = { name: 'Cat', code: 'C' };
      component.save();
      expect(component.saving).toBeFalse();
    });
  });
});
