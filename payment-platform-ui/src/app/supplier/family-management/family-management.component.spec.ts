// @ts-nocheck
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { FamilyManagementComponent } from './family-management.component';
import { CatalogService } from '../../services/catalog.service';
import { ToastService } from '../../services/toast.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('FamilyManagementComponent', () => {
  let component: FamilyManagementComponent;
  let fixture: ComponentFixture<FamilyManagementComponent>;
  let catalogService: jasmine.SpyObj<CatalogService>;
  let toast: jasmine.SpyObj<ToastService>;

  beforeEach(() => {
    sessionStorage.setItem('user', JSON.stringify({ organizationId: 1 }));
    const catalogSpy = jasmine.createSpyObj('CatalogService', ['listCategories', 'listFamilies', 'createFamily', 'updateFamily', 'deleteFamily']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    catalogSpy.listCategories.and.returnValue(of([]));
    catalogSpy.listFamilies.and.returnValue(of([]));

    TestBed.configureTestingModule({
    declarations: [FamilyManagementComponent],
    schemas: [NO_ERRORS_SCHEMA],
    imports: [FormsModule],
    providers: [
        { provide: CatalogService, useValue: catalogSpy },
        { provide: ToastService, useValue: toastSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(FamilyManagementComponent);
    component = fixture.componentInstance;
    catalogService = TestBed.inject(CatalogService) as jasmine.SpyObj<CatalogService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  afterEach(() => sessionStorage.clear());

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should load categories and families', () => {
      catalogService.listCategories.and.returnValue(of([{ id: 1, name: 'Cat1' } as any]));
      catalogService.listFamilies.and.returnValue(of([{ id: 1, name: 'Fam1' } as any]));
      component.ngOnInit();
      expect(component.categories.length).toBe(1);
      expect(component.families.length).toBe(1);
      expect(component.loading).toBeFalse();
    });

    it('should handle categories error', () => {
      catalogService.listCategories.and.returnValue(throwError(() => new Error('fail')));
      component.ngOnInit();
      expect(component.categories.length).toBe(0);
    });

    it('should handle families error', () => {
      catalogService.listFamilies.and.returnValue(throwError(() => new Error('fail')));
      component.ngOnInit();
      expect(component.families.length).toBe(0);
      expect(component.loading).toBeFalse();
    });
  });

  describe('supplierId', () => {
    it('should return organizationId', () => {
      expect(component.supplierId).toBe(1);
    });

    it('should return empty when no user', () => {
      sessionStorage.clear();
      expect(component.supplierId).toBe('');
    });
  });

  describe('getCategoryNames', () => {
    it('should return dash when no categories', () => {
      expect(component.getCategoryNames({ categories: [] } as any)).toBe('-');
    });

    it('should return dash when categories is undefined', () => {
      expect(component.getCategoryNames({} as any)).toBe('-');
    });

    it('should join category names', () => {
      expect(component.getCategoryNames({ categories: [{ name: 'A' }, { name: 'B' }] } as any)).toBe('A, B');
    });

    it('should return single name', () => {
      expect(component.getCategoryNames({ categories: [{ name: 'Only' }] } as any)).toBe('Only');
    });
  });

  describe('toggleCategory', () => {
    it('should add category', () => {
      component.form.categoryIds = [];
      component.toggleCategory(1);
      expect(component.form.categoryIds).toEqual([1]);
    });

    it('should remove category', () => {
      component.form.categoryIds = [1, 2];
      component.toggleCategory(1);
      expect(component.form.categoryIds).toEqual([2]);
    });
  });

  describe('isCategorySelected', () => {
    it('should return true if selected', () => {
      component.form.categoryIds = [1, 2];
      expect(component.isCategorySelected(1)).toBeTrue();
    });

    it('should return false if not selected', () => {
      component.form.categoryIds = [1, 2];
      expect(component.isCategorySelected(3)).toBeFalse();
    });
  });

  describe('openCreate', () => {
    it('should open create form', () => {
      component.openCreate();
      expect(component.showForm).toBeTrue();
      expect(component.editingFamily).toBeNull();
      expect(component.form.name).toBe('');
    });
  });

  describe('openEdit', () => {
    it('should populate form from family', () => {
      component.openEdit({ id: 1, name: 'Fam', code: 'FC', categories: [{ id: 1 } as any] } as any);
      expect(component.editingFamily).toBeTruthy();
      expect(component.form.name).toBe('Fam');
      expect(component.form.code).toBe('FC');
      expect(component.form.categoryIds).toEqual([1]);
    });

    it('should handle family with no categories', () => {
      component.openEdit({ id: 1, name: 'Fam', code: 'FC', categories: null } as any);
      expect(component.form.categoryIds).toEqual([]);
    });
  });

  describe('save', () => {
    it('should create family', () => {
      catalogService.createFamily.and.returnValue(of({} as any));
      component.form = { name: 'New', code: 'N', categoryIds: [] };
      component.save();
      expect(toast.success).toHaveBeenCalledWith('Famille creee');
      expect(component.showForm).toBeFalse();
    });

    it('should not save with empty name', () => {
      component.form = { name: '', code: 'N', categoryIds: [] };
      component.save();
      expect(catalogService.createFamily).not.toHaveBeenCalled();
    });

    it('should not save with empty code', () => {
      component.form = { name: 'New', code: '', categoryIds: [] };
      component.save();
      expect(catalogService.createFamily).not.toHaveBeenCalled();
    });

    it('should not save with whitespace name', () => {
      component.form = { name: '  ', code: 'N', categoryIds: [] };
      component.save();
      expect(catalogService.createFamily).not.toHaveBeenCalled();
    });

    it('should update family when editing', () => {
      catalogService.updateFamily.and.returnValue(of({} as any));
      component.editingFamily = { id: 1 } as any;
      component.form = { name: 'Updated', code: 'U', categoryIds: [1] };
      component.save();
      expect(toast.success).toHaveBeenCalledWith('Famille mise a jour');
    });

    it('should handle create error', () => {
      catalogService.createFamily.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
      component.form = { name: 'New', code: 'N', categoryIds: [] };
      component.save();
      expect(toast.error).toHaveBeenCalledWith('Err');
    });

    it('should handle create error without message', () => {
      catalogService.createFamily.and.returnValue(throwError(() => ({})));
      component.form = { name: 'New', code: 'N', categoryIds: [] };
      component.save();
      expect(toast.error).toHaveBeenCalledWith('Erreur');
    });

    it('should handle update error', () => {
      catalogService.updateFamily.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
      component.editingFamily = { id: 1 } as any;
      component.form = { name: 'Updated', code: 'U', categoryIds: [] };
      component.save();
      expect(toast.error).toHaveBeenCalledWith('Err');
    });
  });

  describe('deleteFamily', () => {
    it('should delete on confirm', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      catalogService.deleteFamily.and.returnValue(of(undefined as any));
      component.deleteFamily({ id: 1, name: 'Fam' } as any);
      expect(catalogService.deleteFamily).toHaveBeenCalledWith(1);
      expect(toast.success).toHaveBeenCalledWith('Famille supprimee');
    });

    it('should not delete when not confirmed', () => {
      spyOn(window, 'confirm').and.returnValue(false);
      component.deleteFamily({ id: 1, name: 'Fam' } as any);
      expect(catalogService.deleteFamily).not.toHaveBeenCalled();
    });

    it('should handle delete error', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      catalogService.deleteFamily.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
      component.deleteFamily({ id: 1, name: 'Fam' } as any);
      expect(toast.error).toHaveBeenCalledWith('Err');
    });
  });
});
