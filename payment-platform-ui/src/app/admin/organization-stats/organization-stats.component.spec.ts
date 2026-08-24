import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { OrganizationStatsComponent } from './organization-stats.component';

describe('OrganizationStatsComponent', () => {
  let component: OrganizationStatsComponent;
  let fixture: ComponentFixture<OrganizationStatsComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [OrganizationStatsComponent]
    });
    fixture = TestBed.createComponent(OrganizationStatsComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have initial state', () => {
    expect(component.stats).toBeNull();
    expect(component.loading).toBeTrue();
  });
});
