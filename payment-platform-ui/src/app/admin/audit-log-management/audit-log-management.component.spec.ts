// @ts-nocheck
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { AuditLogManagementComponent } from './audit-log-management.component';
import { AuditLogService } from '../../services/audit-log.service';
import { AuditLogPage } from '../../models/audit-log.model';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('AuditLogManagementComponent', () => {
  let component: AuditLogManagementComponent;
  let fixture: ComponentFixture<AuditLogManagementComponent>;
  let auditService: jasmine.SpyObj<AuditLogService>;

  const mockPage: AuditLogPage = {
    content: [
      { id: '1', userId: 'u1', organizationId: 'o1', action: 'USER_CREATED', entityId: 'e1', timestamp: '2025-01-01T10:00:00Z', details: 'Created user' },
      { id: '2', userId: 'u2', organizationId: 'o2', action: 'PAYMENT_CONFIRMED', entityId: 'e2', timestamp: '2025-01-02T12:00:00Z', details: 'Confirmed payment' }
    ],
    totalElements: 2,
    totalPages: 1,
    number: 0,
    size: 50
  };

  beforeEach(() => {
    const spy = jasmine.createSpyObj('AuditLogService', ['list']);
    spy.list.and.callFake((params: any) => of({ ...mockPage, number: params?.page ?? 0 }));

    TestBed.configureTestingModule({
    declarations: [AuditLogManagementComponent],
    imports: [FormsModule],
    providers: [
        { provide: AuditLogService, useValue: spy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(AuditLogManagementComponent);
    component = fixture.componentInstance;
    auditService = TestBed.inject(AuditLogService) as jasmine.SpyObj<AuditLogService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load logs on init', () => {
    component.ngOnInit();
    expect(component.logs.length).toBe(2);
    expect(component.loading).toBeFalse();
    expect(component.totalElements).toBe(2);
  });

  it('should handle load error', () => {
    auditService.list.and.returnValue(throwError(() => ({ error: { message: 'Load failed' } })));
    component.loadLogs();
    expect(component.error).toBe('Load failed');
    expect(component.loading).toBeFalse();
  });

  it('should apply filter and reset to first page', () => {
    component.currentPage = 3;
    component.filterAction = 'USER_CREATED';
    component.applyFilter();
    expect(component.currentPage).toBe(0);
    expect(auditService.list).toHaveBeenCalled();
  });

  it('should clear filters and reload', () => {
    component.filterUserId = 'test';
    component.filterAction = 'PAYMENT_CREATED';
    component.filterDateFrom = '2025-01-01';
    component.filterDateTo = '2025-12-31';
    component.clearFilters();
    expect(component.filterUserId).toBe('');
    expect(component.filterAction).toBe('');
    expect(component.filterDateFrom).toBe('');
    expect(component.filterDateTo).toBe('');
    expect(component.currentPage).toBe(0);
  });

  it('should go to next page', () => {
    component.totalPages = 3;
    component.currentPage = 0;
    component.nextPage();
    expect(component.currentPage).toBe(1);
  });

  it('should not go beyond last page', () => {
    component.totalPages = 1;
    component.currentPage = 0;
    component.nextPage();
    expect(component.currentPage).toBe(0);
  });

  it('should go to previous page', () => {
    component.currentPage = 1;
    component.prevPage();
    expect(component.currentPage).toBe(0);
  });

  it('should not go before first page', () => {
    component.currentPage = 0;
    component.prevPage();
    expect(component.currentPage).toBe(0);
  });

  it('should format action names', () => {
    expect(component.formatAction('PAYMENT_CREATED')).toBe('Payment Created');
    expect(component.formatAction('USER_DISABLED')).toBe('User Disabled');
  });
});
