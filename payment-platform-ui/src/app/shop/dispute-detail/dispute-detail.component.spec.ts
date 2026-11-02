// @ts-nocheck
/**
 * Tests du composant DisputeDetailComponent.
 * Perimetre : chargement, droits par role, messages, resolution/cloture, navigation unifiee.
 * Moyens : TestBed + stubs de services, observables RxJS mockes.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { DisputeDetailComponent } from './dispute-detail.component';
import { DisputeService } from '../../services/dispute.service';
import { LoginService } from '../../services/login.service';
import { ToastService } from '../../services/toast.service';
import { ConfirmDialogService } from '../../components/confirm-dialog/confirm-dialog.service';
import { translateServiceProvider, TranslateStubPipe } from '../../testing/translate-stubs';

describe('DisputeDetailComponent', () => {
  let component: DisputeDetailComponent;
  let fixture: ComponentFixture<DisputeDetailComponent>;
  let disputeService: jasmine.SpyObj<DisputeService>;
  let loginService: jasmine.SpyObj<LoginService>;
  let toast: jasmine.SpyObj<ToastService>;
  let confirmDialog: jasmine.SpyObj<ConfirmDialogService>;
  let router: jasmine.SpyObj<Router>;

  const dispute = {
    id: 'd1', orderId: 'o1', shopId: 'shop-1', supplierId: 'sup-1',
    openedBy: 'u1', status: 'OPEN', reason: 'Produit endommage',
    messages: [], version: 1, createdAt: '', updatedAt: ''
  } as any;

  function setRoles(roles: string[]) {
    loginService.getCurrentUser.and.returnValue({ roles, organizationId: 'shop-1', userId: 'u1' } as any);
  }

  function configure(roles: string[], listRoute: string | null = '/dashboard/reclamations') {
    setRoles(roles);
    TestBed.configureTestingModule({
      declarations: [DisputeDetailComponent, TranslateStubPipe],
      schemas: [NO_ERRORS_SCHEMA],
      imports: [],
      providers: [
        translateServiceProvider(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: () => 'd1' },
              data: listRoute ? { listRoute } : {}
            }
          }
        },
        { provide: Router, useValue: router },
        { provide: DisputeService, useValue: disputeService },
        { provide: LoginService, useValue: loginService },
        { provide: ToastService, useValue: toast },
        { provide: ConfirmDialogService, useValue: confirmDialog },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    fixture = TestBed.createComponent(DisputeDetailComponent);
    component = fixture.componentInstance;
  }

  beforeEach(() => {
    disputeService = jasmine.createSpyObj('DisputeService', [
      'getById', 'addMessage', 'resolve', 'transition', 'updatePriority',
      'assign', 'addNote', 'uploadAttachment', 'downloadAttachment'
    ]);
    loginService = jasmine.createSpyObj('LoginService', ['getCurrentUser']);
    toast = jasmine.createSpyObj('ToastService', ['success', 'error']);
    confirmDialog = jasmine.createSpyObj('ConfirmDialogService', ['confirm']);
    router = jasmine.createSpyObj('Router', ['navigate']);
    router.url = '/dashboard/reclamations/d1';

    disputeService.getById.and.returnValue(of(dispute));
    disputeService.addMessage.and.returnValue(of({ ...dispute, messages: [{ content: 'Message' }] }));
    disputeService.resolve.and.returnValue(of({ ...dispute, status: 'RESOLVED' }));
    disputeService.transition.and.returnValue(of({ ...dispute, status: 'IN_PROGRESS' }));
    disputeService.updatePriority.and.returnValue(of({ ...dispute, priority: 'URGENT' }));
    disputeService.assign.and.returnValue(of({ ...dispute, assignedTo: 'u1' }));
    disputeService.addNote.and.returnValue(of(dispute));
    confirmDialog.confirm.and.returnValue(of(true));
  });

  it('should load the dispute on init', () => {
    configure(['SHOP_ADMIN']);
    component.ngOnInit();
    expect(disputeService.getById).toHaveBeenCalledWith('d1');
    expect(component.dispute?.id).toBe('d1');
    expect(component.loading).toBeFalse();
    expect(component.qrData).toContain('/dashboard/reclamations/d1');
  });

  it('should redirect to the unified list when loading fails', () => {
    configure(['SHOP_ADMIN']);
    disputeService.getById.and.returnValue(throwError(() => ({ status: 403 })));
    component.ngOnInit();
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard/reclamations']);
  });

  it('should allow messaging for shop and supplier agents', () => {
    configure(['SHOP_AGENT']);
    expect(component.canSendMessage()).toBeTrue();

    setRoles(['SUPPLIER_AGENT']);
    expect(component.canSendMessage()).toBeTrue();

    setRoles(['SYSTEM_ADMIN']);
    expect(component.canSendMessage()).toBeFalse();
  });

  it('should allow resolution for admins only', () => {
    configure(['SHOP_ADMIN']);
    expect(component.canResolve()).toBeTrue();

    setRoles(['SUPPLIER_ADMIN']);
    expect(component.canResolve()).toBeTrue();

    setRoles(['SYSTEM_ADMIN']);
    expect(component.canResolve()).toBeTrue();

    setRoles(['SHOP_AGENT']);
    expect(component.canResolve()).toBeFalse();
  });

  it('should send a trimmed message and update the dispute', () => {
    configure(['SHOP_ADMIN']);
    component.ngOnInit();
    component.newMessage = '  Message  ';

    component.sendMessage();

    expect(disputeService.addMessage).toHaveBeenCalledWith('d1', { content: 'Message' });
    expect(component.newMessage).toBe('');
    expect(component.submitting).toBeFalse();
    expect(toast.success).toHaveBeenCalledWith('DISPUTES.MESSAGE_SENT');
  });

  it('should not send an empty message', () => {
    configure(['SHOP_ADMIN']);
    component.ngOnInit();
    component.newMessage = '   ';

    component.sendMessage();

    expect(disputeService.addMessage).not.toHaveBeenCalled();
  });

  it('should resolve the dispute after confirmation', () => {
    configure(['SHOP_ADMIN']);
    component.ngOnInit();

    component.resolveDispute();

    expect(confirmDialog.confirm).toHaveBeenCalled();
    expect(disputeService.resolve).toHaveBeenCalledWith('d1', 'RESOLVED');
    expect(toast.success).toHaveBeenCalledWith('DISPUTES.RESOLVED_MSG');
  });

  it('should close the dispute after confirmation', () => {
    configure(['SYSTEM_ADMIN']);
    component.ngOnInit();

    component.closeDispute();

    expect(disputeService.resolve).toHaveBeenCalledWith('d1', 'CLOSED');
    expect(toast.success).toHaveBeenCalledWith('DISPUTES.CLOSED_MSG');
  });

  it('should not resolve when confirmation is cancelled', () => {
    configure(['SHOP_ADMIN']);
    component.ngOnInit();
    confirmDialog.confirm.and.returnValue(of(false));

    component.resolveDispute();

    expect(disputeService.resolve).not.toHaveBeenCalled();
  });

  it('should always navigate back to the unified list', () => {
    configure(['SYSTEM_ADMIN']);
    component.backToOrder();
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard/reclamations']);
  });

  it('should fall back to the unified list without configured route data', () => {
    configure(['SHOP_AGENT'], null);
    component.backToOrder();
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard/reclamations']);
  });

  it('should expose labels and status classes', () => {
    configure(['SHOP_ADMIN']);
    expect(component.statusLabel('OPEN')).not.toBe('STATUS.OPEN');
    expect(component.statusClass('OPEN')).toBe('open');
    expect(component.statusClass('IN_PROGRESS')).toBe('in-progress');
    expect(component.statusClass('UNKNOWN')).toBe('');
    expect(component.senderLabel({ senderRole: 'SUPPLIER' })).toBe('DISPUTES.SENDER_SUPPLIER');
    expect(component.senderLabel({ senderRole: 'SHOP' })).toBe('DISPUTES.SENDER_SHOP');
    expect(component.priorityLabel('URGENT')).toBe('DISPUTES.PRIORITY_URGENT');
    expect(component.priorityClass('HIGH')).toBe('priority-high');
  });

  it('should take charge of an open dispute for admins', () => {
    configure(['SHOP_ADMIN']);
    component.ngOnInit();
    component.takeCharge();
    expect(disputeService.transition).toHaveBeenCalledWith('d1', 'IN_PROGRESS');
    expect(component.dispute?.status).toBe('IN_PROGRESS');
    expect(toast.success).toHaveBeenCalledWith('DISPUTES.TAKEN_CHARGE_MSG');
  });

  it('should update priority and assign the dispute', () => {
    configure(['SHOP_ADMIN']);
    component.ngOnInit();
    component.priorityDraft = 'URGENT';
    component.savePriority();
    expect(disputeService.updatePriority).toHaveBeenCalledWith('d1', 'URGENT');

    component.assignToMe();
    expect(disputeService.assign).toHaveBeenCalledWith('d1');
    expect(component.dispute?.assignedTo).toBe('u1');
  });

  it('should add an internal note and reload the dispute', () => {
    configure(['SHOP_ADMIN']);
    component.ngOnInit();
    component.noteContent = '  Relance fournisseur  ';
    component.addNote();
    expect(disputeService.addNote).toHaveBeenCalledWith('d1', 'Relance fournisseur');
    expect(disputeService.getById).toHaveBeenCalledTimes(2);
    expect(toast.success).toHaveBeenCalledWith('DISPUTES.NOTE_ADDED');
  });

  it('should detect overdue SLAs', () => {
    configure(['SHOP_ADMIN']);
    component.dispute = { ...dispute, status: 'OPEN', dueAt: '2000-01-01T00:00:00Z' } as any;
    expect(component.isOverdue()).toBeTrue();
    component.dispute = { ...dispute, status: 'CLOSED', dueAt: '2000-01-01T00:00:00Z' } as any;
    expect(component.isOverdue()).toBeFalse();
  });
});
