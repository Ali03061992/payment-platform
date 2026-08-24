import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ShopManagementComponent } from './shop-management.component';

describe('ShopManagementComponent', () => {
  let component: ShopManagementComponent;
  let fixture: ComponentFixture<ShopManagementComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [ShopManagementComponent]
    });
    fixture = TestBed.createComponent(ShopManagementComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have initial state', () => {
    expect(component.shops).toEqual([]);
    expect(component.loading).toBeTrue();
    expect(component.showCreate).toBeFalse();
    expect(component.newName).toBe('');
  });
});
