import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PaymentService } from '../../services/payment.service';
import { Payment } from '../../models/payment.model';
import { ToastService } from '../../services/toast.service';
import { environment } from '../../../environments/environment';
import { Subscription } from 'rxjs';

@Component({
    selector: 'app-payment-detail',
    templateUrl: './payment-detail.component.html',
    styleUrls: ['./payment-detail.component.css'],
    standalone: false
})
export class PaymentDetailComponent implements OnInit, OnDestroy {
  payment: Payment | null = null;
  loading = true;
  showReject = false;
  showQR = false;
  rejectReason = '';
  qrData = '';
  canShare = typeof navigator !== 'undefined' && 'share' in navigator;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private paymentService: PaymentService,
    private toast: ToastService
  ) {}

  private subscriptions = new Subscription();

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id);
    const request$ = isUuid
      ? this.paymentService.getById(id)
      : this.paymentService.getByReference(id);
    this.subscriptions.add(request$.subscribe({
      next: (data: Payment) => {
        this.payment = data;
        this.qrData = `${environment.appUrl}/dashboard/payments/${data.id}`;
        this.loading = false;
      },
      error: () => { this.loading = false; this.router.navigate(['/dashboard/payments']); }
    }));
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  confirm(): void {
    if (!this.payment) return;
    this.subscriptions.add(this.paymentService.confirm(this.payment.id).subscribe({
      next: (data: Payment) => { this.payment = data; this.toast.success('Paiement confirmé'); },
      error: (e: any) => { this.toast.error(e.error?.message || 'Erreur'); }
    }));
  }

  reject(): void {
    if (!this.payment || !this.rejectReason.trim()) return;
    this.subscriptions.add(this.paymentService.reject(this.payment.id, { rejectionReason: this.rejectReason }).subscribe({
      next: (data: Payment) => { this.payment = data; this.showReject = false; this.rejectReason = ''; this.toast.success('Paiement rejeté'); },
      error: (e: any) => { this.toast.error(e.error?.message || 'Erreur'); }
    }));
  }

  cancel(): void {
    if (!this.payment) return;
    this.subscriptions.add(this.paymentService.cancel(this.payment.id).subscribe({
      next: (data: Payment) => { this.payment = data; this.toast.success('Paiement annulé'); },
      error: (e: any) => { this.toast.error(e.error?.message || 'Erreur'); }
    }));
  }

  toggleQR(): void {
    this.showQR = !this.showQR;
  }

  shareQR(): void {
    if (navigator.share && this.payment) {
      navigator.share({
        title: `Paiement ${this.payment.reference}`,
        text: `Facture ${this.payment.reference} - ${this.payment.amount} ${this.payment.currency}`,
        url: this.qrData
      }).catch(() => {});
    }
  }

  statusLabel(s: string): string {
    const map: Record<string, string> = { PENDING: 'En attente', CONFIRMED: 'Confirmé', REJECTED: 'Rejeté', CANCELLED: 'Annulé' };
    return map[s] || s;
  }

  actionLabel(a: string): string {
    const map: Record<string, string> = {
      PAYMENT_CREATED: 'Créé', PAYMENT_CONFIRMED: 'Confirmé',
      PAYMENT_REJECTED: 'Rejeté', PAYMENT_CANCELLED: 'Annulé'
    };
    return map[a] || a;
  }
}
