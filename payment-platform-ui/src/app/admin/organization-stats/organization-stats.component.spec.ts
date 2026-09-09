import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';
import { OrganizationStatsComponent } from './organization-stats.component';
import { OrganizationService } from '../../services/organization.service';

describe('OrganizationStatsComponent', () => {
  let component: OrganizationStatsComponent;
  let fixture: ComponentFixture<OrganizationStatsComponent>;
  let orgService: jasmine.SpyObj<OrganizationService>;

  beforeEach(() => {
    const orgSpy = jasmine.createSpyObj('OrganizationService', ['getStats']);
    orgSpy.getStats.and.returnValue(of({ totalSuppliers: 5, totalShops: 10 } as any));

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [OrganizationStatsComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: OrganizationService, useValue: orgSpy }
      ]
    });
    fixture = TestBed.createComponent(OrganizationStatsComponent);
    component = fixture.componentInstance;
    orgService = TestBed.inject(OrganizationService) as jasmine.SpyObj<OrganizationService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load stats on init', () => {
    component.ngOnInit();
    expect(component.stats).toBeTruthy();
    expect(component.loading).toBeFalse();
  });

  it('should handle load error', () => {
    orgService.getStats.and.returnValue(throwError(() => new Error('fail')));
    component.ngOnInit();
    expect(component.loading).toBeFalse();
    expect(component.stats).toBeNull();
  });
});
