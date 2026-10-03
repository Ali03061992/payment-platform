import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DisputeService } from '../../services/dispute.service';
import { LoginService } from '../../services/login.service';
import { Dispute } from '../../models/dispute.model';
import { ToastService } from '../../services/toast.service';
import { ConfirmDialogService } from '../../components/confirm-dialog/confirm-dialog.service';
import { Subscription } from 'rxjs';

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
    private loginService: LoginService
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
        this.toast.success('Message envoyé');
      },
      error: (e: any) => { this.submitting = false; this.toast.error(e.error?.message || 'Erreur'); }
    }));
  }

  resolveDispute(): void {
    if (!this.dispute) return;
    const id = this.dispute.id;
    this.subscriptions.add(this.confirmDialog.confirm({
      title: 'Marquer comme résolu',
      message: 'Marquer ce litige comme résolu ?',
    }).subscribe(ok => {
      if (!ok) return;
      this.subscriptions.add(this.disputeService.resolve(id, 'RESOLVED').subscribe({
        next: (data: Dispute) => { this.dispute = data; this.toast.success('Litige résolu'); },
        error: (e: any) => { this.toast.error(e.error?.message || 'Erreur'); }
      }));
    }));
  }

  closeDispute(): void {
    if (!this.dispute) return;
    const id = this.dispute.id;
    this.subscriptions.add(this.confirmDialog.confirm({
      title: 'Fermer le litige',
      message: 'Fermer ce litige ?',
      danger: true,
    }).subscribe(ok => {
      if (!ok) return;
      this.subscriptions.add(this.disputeService.resolve(id, 'CLOSED').subscribe({
        next: (data: Dispute) => { this.dispute = data; this.toast.success('Litige fermé'); },
        error: (e: any) => { this.toast.error(e.error?.message || 'Erreur'); }
      }));
    }));
  }

  statusLabel(s: string): string {
    const map: Record<string, string> = {
      OPEN: 'Ouvert', IN_PROGRESS: 'En cours',
      RESOLVED: 'Résolu', CLOSED: 'Fermé'
    };
    return map[s] || s;
  }

  statusClass(s: string): string {
    const map: Record<string, string> = {
      OPEN: 'open', IN_PROGRESS: 'in-progress',
      RESOLVED: 'resolved', CLOSED: 'closed'
    };
    return map[s] || '';
  }

  senderLabel(msg: any): string {
    return msg.senderRole === 'SUPPLIER' ? 'Fournisseur' : 'Boutique';
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
