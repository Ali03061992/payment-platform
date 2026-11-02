// @ts-nocheck
/**
 * Tests du composant ReclamationsComponent.
 * Perimetre : chargement des litiges, formulaire de creation, validation, navigation.
 * Moyens : TestBed + stubs de services, observables RxJS mockes.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ReclamationsComponent } from './reclamations.component';
import { DisputeService } from '../../services/dispute.service';
import { OrderService } from '../../services/order.service';
import { ToastService } from '../../services/toast.service';
import { translateServiceProvider, TranslateStubPipe } from '../../testing/translate-stubs';

describe('ReclamationsComponent', () => {
  let component: ReclamationsComponent;
  let fixture: ComponentFixture<ReclamationsComponent>;
  let disputeService: jasmine.SpyObj<DisputeService>;
  let orderService: jasmine.SpyObj<OrderService>;
  let toast: jasmine.SpyObj<ToastService>;
  let router: jasmine.SpyObj<Router>;

  const dispute = {
    id: 'd1', orderId: 'o1', shopId: 'shop-1', supplierId: 'sup-1',
    openedBy: 'u1', status: 'OPEN', reason: 'Produit endommage',
    messages: [], version: 1, createdAt: '', updatedAt: ''
  } as any;

  const eligibleOrder = { id: 'o1', reference: 'CMD-001', status: 'REJECTED' } as any;

  beforeEach(() => {
    disputeService = jasmine.createSpyObj('DisputeService', ['listPaged', 'create']);
    orderService = jasmine.createSpyObj('OrderService', ['listEligibleForDispute']);
    toast = jasmine.createSpyObj('ToastService', ['success', 'error']);
    router = jasmine.createSpyObj('Router', ['navigate']);

    disputeService.listPaged.and.returnValue(of({
      items: [dispute], totalElements: 1, totalPages: 1, number: 0
    }));
    disputeService.create.and.returnValue(of(dispute));
    orderService.listEligibleForDispute.and.returnValue(of([eligibleOrder]));

    TestBed.configureTestingModule({
      declarations: [ReclamationsComponent, TranslateStubPipe],
      schemas: [NO_ERRORS_SCHEMA],
      imports: [],
      providers: [
        translateServiceProvider(),
        { provide: DisputeService, useValue: disputeService },
        { provide: OrderService, useValue: orderService },
        { provide: ToastService, useValue: toast },
        { provide: Router, useValue: router },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    fixture = TestBed.createComponent(ReclamationsComponent);
    component = fixture.componentInstance;
  });

  it('should create and load disputes on init', () => {
    component.ngOnInit();
    expect(component.disputes.length).toBe(1);
    expect(component.loading).toBeFalse();
  });

  it('should display an error when loading disputes fails', () => {
    disputeService.listPaged.and.returnValue(throwError(() => ({ error: { message: 'Erreur API' } })));
    component.load();
    expect(toast.error).toHaveBeenCalledWith('Erreur API');
    expect(component.loading).toBeFalse();
  });

  it('should load eligible orders when opening the form', () => {
    component.openForm();
    expect(component.showForm).toBeTrue();
    expect(orderService.listEligibleForDispute).toHaveBeenCalledWith(0, 100);
    expect(component.eligibleOrders.length).toBe(1);
    expect(component.loadingOrders).toBeFalse();
  });

  it('should fall back to an empty list when eligible orders fail to load', () => {
    orderService.listEligibleForDispute.and.returnValue(throwError(() => ({})));
    component.openForm();
    expect(component.eligibleOrders).toEqual([]);
    expect(toast.error).toHaveBeenCalledWith('RECLAMATIONS.ORDERS_LOAD_ERROR');
  });

  it('should validate the creation form', () => {
    expect(component.canSubmit()).toBeFalse();

    component.selectedOrderId = 'o1';
    expect(component.canSubmit()).toBeFalse();

    component.reason = '   ';
    expect(component.canSubmit()).toBeFalse();

    component.reason = 'a'.repeat(256);
    expect(component.canSubmit()).toBeFalse();

    component.reason = 'Motif valide';
    expect(component.canSubmit()).toBeTrue();
  });

  it('should create a dispute with a trimmed reason and reload the list', () => {
    component.ngOnInit();
    component.showForm = true;
    component.selectedOrderId = 'o1';
    component.reason = '  Produit endommage  ';

    component.create();

    expect(disputeService.create).toHaveBeenCalledWith({
      orderId: 'o1',
      reason: 'Produit endommage',
      priority: 'NORMAL'
    });
    expect(toast.success).toHaveBeenCalledWith('RECLAMATIONS.CREATED');
    expect(component.showForm).toBeFalse();
    expect(component.submitting).toBeFalse();
    expect(disputeService.listPaged).toHaveBeenCalledTimes(2);
  });

  it('should forward filters and pagination to the API', () => {
    component.searchTerm = 'endommage';
    component.statusFilter = 'OPEN';
    component.priorityFilter = 'HIGH';
    component.onFilterChange();

    expect(disputeService.listPaged).toHaveBeenCalledWith({
      status: 'OPEN',
      priority: 'HIGH',
      search: 'endommage',
      page: 0,
      size: 10
    });
  });

  it('should reset filters and paginate', () => {
    component.page = 1;
    component.totalPages = 3;
    disputeService.listPaged.and.returnValue(of({
      items: [], totalElements: 0, totalPages: 3, number: 2
    }));
    component.nextPage();
    expect(component.page).toBe(2);

    disputeService.listPaged.and.returnValue(of({
      items: [], totalElements: 0, totalPages: 3, number: 1
    }));
    component.previousPage();
    expect(component.page).toBe(1);

    component.searchTerm = 'x';
    disputeService.listPaged.and.returnValue(of({
      items: [], totalElements: 0, totalPages: 1, number: 0
    }));
    component.resetFilters();
    expect(component.searchTerm).toBe('');
    expect(component.page).toBe(0);
  });

  it('should expose priority helpers and overdue status', () => {
    expect(component.priorityLabel('URGENT')).toBe('DISPUTES.PRIORITY_URGENT');
    expect(component.priorityClass('HIGH')).toBe('priority-high');
    const overdue = { ...dispute, status: 'OPEN', dueAt: '2000-01-01T00:00:00Z' };
    expect(component.isOverdue(overdue)).toBeTrue();
  });

  it('should not create a dispute when the form is invalid', () => {
    component.create();
    expect(disputeService.create).not.toHaveBeenCalled();
  });

  it('should display an error when creation fails', () => {
    disputeService.create.and.returnValue(throwError(() => ({ error: { message: 'Conflit' } })));
    component.selectedOrderId = 'o1';
    component.reason = 'Motif';

    component.create();

    expect(toast.error).toHaveBeenCalledWith('Conflit');
    expect(component.submitting).toBeFalse();
  });

  it('should navigate to the unified dispute detail', () => {
    component.openDispute('d1');
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard/reclamations', 'd1']);
  });

  it('should label orders and statuses', () => {
    expect(component.orderLabel(eligibleOrder)).toContain('CMD-001');
    expect(component.statusClass('OPEN')).toBe('pending');
    expect(component.statusClass('IN_PROGRESS')).toBe('confirmed');
    expect(component.statusClass('UNKNOWN')).toBe('');
  });
});
