import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RelationManagementComponent } from './relation-management.component';

describe('RelationManagementComponent', () => {
  let component: RelationManagementComponent;
  let fixture: ComponentFixture<RelationManagementComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [RelationManagementComponent]
    });
    fixture = TestBed.createComponent(RelationManagementComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have initial state', () => {
    expect(component.relations).toEqual([]);
    expect(component.suppliers).toEqual([]);
    expect(component.shops).toEqual([]);
    expect(component.loading).toBeTrue();
    expect(component.showCreate).toBeFalse();
  });

  describe('getSupplierName', () => {
    it('should return name when found', () => {
      component.suppliers = [{ id: 1, name: 'Supplier A', type: 'SUPPLIER', status: 'ACTIVE', version: 1, createdAt: '', updatedAt: '', relations: [] }];
      expect(component.getSupplierName(1)).toBe('Supplier A');
    });

    it('should return fallback when not found', () => {
      expect(component.getSupplierName(999)).toBe('#999');
    });
  });

  describe('getShopName', () => {
    it('should return name when found', () => {
      component.shops = [{ id: 2, name: 'Shop B', type: 'SHOP', status: 'ACTIVE', version: 1, createdAt: '', updatedAt: '', relations: [] }];
      expect(component.getShopName(2)).toBe('Shop B');
    });

    it('should return fallback when not found', () => {
      expect(component.getShopName(888)).toBe('#888');
    });
  });
});
