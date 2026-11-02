import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DisputeService } from '../../services/dispute.service';
import { LoginService } from '../../services/login.service';
import { Dispute, DisputeAttachment, DisputePriority } from '../../models/dispute.model';
import { environment } from '../../../environments/environment';
import { ToastService } from '../../services/toast.service';
import { ConfirmDialogService } from '../../components/confirm-dialog/confirm-dialog.service';
import { Subscription } from 'rxjs';
import { statusLabelFr } from '../../pipes/status-label.pipe';

@Component({
    selector: 'app-dispute-detail',
    templateUrl: './dispute-detail.component.html',
    styleUrls: ['./dispute-detail.component.css'],
    standalone: false
})
export class DisputeDetailComponent implements OnInit, OnDestroy {
  dispute: Dispute | null = null;
  loading = true;
  /** QR de la page : scanné, il réouvre directement ce détail de litige. */
  qrData = '';
  newMessage = '';
  submitting = false;

  readonly priorities: DisputePriority[] = ['LOW', 'NORMAL', 'HIGH', 'URGENT'];
  priorityDraft: DisputePriority = 'NORMAL';
  noteContent = '';
  addingNote = false;
  uploading = false;
  updatingPriority = false;

  private subscriptions = new Subscription();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private disputeService: DisputeService,
    private toast: ToastService,
    private confirmDialog: ConfirmDialogService,
    private loginService: LoginService,
    private translate: TranslateService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.qrData = `${environment.appUrl}${this.router.url}`;
    this.loadDispute(id);
  }

  private loadDispute(id: string): void {
    this.subscriptions.add(this.disputeService.getById(id).subscribe({
      next: (data: Dispute) => {
        this.dispute = data;
        this.priorityDraft = data.priority || 'NORMAL';
        this.loading = false;
      },
      error: () => { this.loading = false; this.router.navigate([this.disputesListRoute()]); }
    }));
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  canSendMessage(): boolean {
    const roles = this.loginService.getCurrentUser()?.roles || [];
    return roles.includes('SHOP_ADMIN') || roles.includes('SHOP_AGENT')
      || roles.includes('SUPPLIER_ADMIN') || roles.includes('SUPPLIER_AGENT');
  }

  canResolve(): boolean {
    return this.isAdmin();
  }

  isAdmin(): boolean {
    const roles = this.loginService.getCurrentUser()?.roles || [];
    return roles.includes('SYSTEM_ADMIN') || roles.includes('SHOP_ADMIN') || roles.includes('SUPPLIER_ADMIN');
  }

  takeCharge(): void {
    if (!this.canResolve() || !this.dispute || this.dispute.status !== 'OPEN') return;
    this.subscriptions.add(this.disputeService.transition(this.dispute.id, 'IN_PROGRESS').subscribe({
      next: (data: Dispute) => {
        this.dispute = data;
        this.toast.success(this.translate.instant('DISPUTES.TAKEN_CHARGE_MSG'));
      },
      error: (e: any) => this.toast.error(e.error?.message || this.translate.instant('COMMON.ERROR'))
    }));
  }

  savePriority(): void {
    if (!this.canResolve() || !this.dispute || this.priorityDraft === this.dispute.priority) return;
    this.updatingPriority = true;
    this.subscriptions.add(this.disputeService.updatePriority(this.dispute.id, this.priorityDraft).subscribe({
      next: (data: Dispute) => {
        this.dispute = data;
        this.updatingPriority = false;
        this.toast.success(this.translate.instant('DISPUTES.PRIORITY_UPDATED'));
      },
      error: (e: any) => {
        this.updatingPriority = false;
        this.toast.error(e.error?.message || this.translate.instant('COMMON.ERROR'));
      }
    }));
  }

  assignToMe(): void {
    if (!this.canResolve() || !this.dispute) return;
    this.subscriptions.add(this.disputeService.assign(this.dispute.id).subscribe({
      next: (data: Dispute) => {
        this.dispute = data;
        this.toast.success(this.translate.instant('DISPUTES.ASSIGNED_MSG'));
      },
      error: (e: any) => this.toast.error(e.error?.message || this.translate.instant('COMMON.ERROR'))
    }));
  }

  addNote(): void {
    if (!this.canResolve() || !this.dispute || !this.noteContent.trim()) return;
    this.addingNote = true;
    const id = this.dispute.id;
    this.subscriptions.add(this.disputeService.addNote(id, this.noteContent.trim()).subscribe({
      next: () => {
        this.noteContent = '';
        this.addingNote = false;
        this.loadDispute(id);
        this.toast.success(this.translate.instant('DISPUTES.NOTE_ADDED'));
      },
      error: (e: any) => {
        this.addingNote = false;
        this.toast.error(e.error?.message || this.translate.instant('COMMON.ERROR'));
      }
    }));
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !this.dispute) return;
    this.uploading = true;
    const id = this.dispute.id;
    this.subscriptions.add(this.disputeService.uploadAttachment(id, file).subscribe({
      next: (data: Dispute) => {
        this.dispute = data;
        this.uploading = false;
        input.value = '';
        this.toast.success(this.translate.instant('DISPUTES.ATTACHMENT_ADDED'));
      },
      error: (e: any) => {
        this.uploading = false;
        input.value = '';
        this.toast.error(e.error?.message || this.translate.instant('COMMON.ERROR'));
      }
    }));
  }

  downloadAttachment(attachment: DisputeAttachment): void {
    if (!this.dispute) return;
    this.subscriptions.add(
      this.disputeService.downloadAttachment(this.dispute.id, attachment.id).subscribe({
        next: (blob: Blob) => {
          const url = URL.createObjectURL(blob);
          const anchor = document.createElement('a');
          anchor.href = url;
          anchor.download = attachment.fileName;
          anchor.click();
          URL.revokeObjectURL(url);
        },
        error: () => this.toast.error(this.translate.instant('COMMON.ERROR'))
      })
    );
  }

  priorityLabel(priority?: string): string {
    return priority ? this.translate.instant(`DISPUTES.PRIORITY_${priority}`) : '';
  }

  priorityClass(priority?: string): string {
    return priority ? `priority-${priority.toLowerCase()}` : '';
  }

  isOverdue(): boolean {
    return !!this.dispute?.dueAt
      && new Date(this.dispute.dueAt).getTime() < Date.now()
      && this.dispute.status !== 'RESOLVED' && this.dispute.status !== 'CLOSED';
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} o`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} Ko`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
  }

  sendMessage(): void {
    if (!this.canSendMessage() || !this.dispute || !this.newMessage.trim()) return;
    this.submitting = true;
    this.subscriptions.add(this.disputeService.addMessage(this.dispute.id, { content: this.newMessage.trim() }).subscribe({
      next: (data: Dispute) => {
        this.dispute = data;
        this.newMessage = '';
        this.submitting = false;
        this.toast.success(this.translate.instant('DISPUTES.MESSAGE_SENT'));
      },
      error: (e: any) => { this.submitting = false; this.toast.error(e.error?.message || this.translate.instant('COMMON.ERROR')); }
    }));
  }

  resolveDispute(): void {
    if (!this.canResolve() || !this.dispute) return;
    const id = this.dispute.id;
    this.subscriptions.add(this.confirmDialog.confirm({
      title: this.translate.instant('DISPUTES.RESOLVE_TITLE'),
      message: this.translate.instant('DISPUTES.RESOLVE_MSG'),
    }).subscribe(ok => {
      if (!ok) return;
      this.subscriptions.add(this.disputeService.resolve(id, 'RESOLVED').subscribe({
        next: (data: Dispute) => { this.dispute = data; this.toast.success(this.translate.instant('DISPUTES.RESOLVED_MSG')); },
        error: (e: any) => { this.toast.error(e.error?.message || this.translate.instant('COMMON.ERROR')); }
      }));
    }));
  }

  closeDispute(): void {
    if (!this.canResolve() || !this.dispute) return;
    const id = this.dispute.id;
    this.subscriptions.add(this.confirmDialog.confirm({
      title: this.translate.instant('DISPUTES.CLOSE_TITLE'),
      message: this.translate.instant('DISPUTES.CLOSE_MSG'),
      danger: true,
    }).subscribe(ok => {
      if (!ok) return;
      this.subscriptions.add(this.disputeService.resolve(id, 'CLOSED').subscribe({
        next: (data: Dispute) => { this.dispute = data; this.toast.success(this.translate.instant('DISPUTES.CLOSED_MSG')); },
        error: (e: any) => { this.toast.error(e.error?.message || this.translate.instant('COMMON.ERROR')); }
      }));
    }));
  }

  statusLabel(s: string): string {
    const key = `STATUS.${s}`;
    const translated = this.translate.instant(key);
    return translated && translated !== key ? translated : statusLabelFr(s);
  }

  statusClass(s: string): string {
    const map: Record<string, string> = {
      OPEN: 'open', IN_PROGRESS: 'in-progress',
      RESOLVED: 'resolved', CLOSED: 'closed'
    };
    return map[s] || '';
  }

  senderLabel(msg: any): string {
    return this.translate.instant(msg.senderRole === 'SUPPLIER' ? 'DISPUTES.SENDER_SUPPLIER' : 'DISPUTES.SENDER_SHOP');
  }

  private disputesListRoute(): string {
    const configured = this.route.snapshot.data['listRoute'];
    return typeof configured === 'string' && configured ? configured : '/dashboard/reclamations';
  }

  backToOrder(): void {
    this.router.navigate([this.disputesListRoute()]);
  }
}