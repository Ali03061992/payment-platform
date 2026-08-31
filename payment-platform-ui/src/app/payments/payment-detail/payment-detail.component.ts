import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PaymentService } from '../../services/payment.service';
import { Payment } from '../../models/payment.model';

@Component({
  selector: 'app-payment-detail',
  templateUrl: './payment-detail.component.html',
  styleUrls: ['./payment-detail.component.css']
})
export class PaymentDetailComponent implements OnInit {
  payment: Payment | null = null;
  loading = true;
  showReject = false;
  showQR = false;
  rejectReason = '';
  errorMsg = '';
  successMsg = '';
  qrData = '';
  canShare = typeof navigator !== 'undefined' && 'share' in navigator;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private paymentService: PaymentService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.paymentService.getById(id).subscribe({
      next: (data: Payment) => {
        this.payment = data;
        this.qrData = `${window.location.origin}/dashboard/payments/${data.id}`;
        this.loading = false;
      },
      error: () => { this.loading = false; this.router.navigate(['/dashboard/payments']); }
    });
  }

  confirm(): void {
    if (!this.payment) return;
    this.paymentService.confirm(this.payment.id).subscribe({
      next: (data: Payment) => { this.payment = data; this.successMsg = 'Paiement confirmé'; },
      error: (e: any) => { this.errorMsg = e.error?.message || 'Erreur'; setTimeout(() => this.errorMsg = '', 3000); }
    });
  }

  reject(): void {
    if (!this.payment || !this.rejectReason.trim()) return;
    this.paymentService.reject(this.payment.id, { rejectionReason: this.rejectReason }).subscribe({
      next: (data: Payment) => { this.payment = data; this.showReject = false; this.rejectReason = ''; this.successMsg = 'Paiement rejeté'; },
      error: (e: any) => { this.errorMsg = e.error?.message || 'Erreur'; setTimeout(() => this.errorMsg = '', 3000); }
    });
  }

  cancel(): void {
    if (!this.payment) return;
    this.paymentService.cancel(this.payment.id).subscribe({
      next: (data: Payment) => { this.payment = data; this.successMsg = 'Paiement annulé'; },
      error: (e: any) => { this.errorMsg = e.error?.message || 'Erreur'; setTimeout(() => this.errorMsg = '', 3000); }
    });
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
