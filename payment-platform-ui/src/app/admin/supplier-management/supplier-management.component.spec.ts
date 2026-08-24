import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { SupplierManagementComponent } from './supplier-management.component';

describe('SupplierManagementComponent', () => {
  let component: SupplierManagementComponent;
  let fixture: ComponentFixture<SupplierManagementComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [SupplierManagementComponent]
    });
    fixture = TestBed.createComponent(SupplierManagementComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have initial state', () => {
    expect(component.suppliers).toEqual([]);
    expect(component.loading).toBeTrue();
    expect(component.showCreate).toBeFalse();
    expect(component.newName).toBe('');
  });
});
