import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { DisputeService } from '../../services/dispute.service';
import { LoginService } from '../../services/login.service';
import { Dispute } from '../../models/dispute.model';
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
  newMessage = '';
  submitting = false;

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
    this.subscriptions.add(this.disputeService.getById(id).subscribe({
      next: (data: Dispute) => { this.dispute = data; this.loading = false; },
      error: () => { this.loading = false; this.router.navigate(['/dashboard/shop/orders']); }
    }));
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  sendMessage(): void {
    if (!this.dispute || !this.newMessage.trim()) return;
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
    if (!this.dispute) return;
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
    if (!this.dispute) return;
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
    return statusLabelFr(s);
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

  backToOrder(): void {
    const roles = this.loginService.getCurrentUser()?.roles || [];
    if (roles.includes('SYSTEM_ADMIN')) {
      this.router.navigate(['/dashboard/admin/disputes']);
      return;
    }
    if (roles.includes('SUPPLIER_ADMIN') || roles.includes('SUPPLIER_AGENT')) {
      this.router.navigate(['/dashboard/supplier/orders']);
      return;
    }
    if (this.dispute) {
      this.router.navigate(['/dashboard/shop/orders', this.dispute.orderId]);
    } else {
      this.router.navigate(['/dashboard/shop/orders']);
    }
  }
}