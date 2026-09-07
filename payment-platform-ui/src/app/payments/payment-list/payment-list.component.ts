import { Component, OnInit, OnDestroy } from '@angular/core';
import { PaymentService } from '../../services/payment.service';
import { Payment } from '../../models/payment.model';
import { ToastService } from '../../services/toast.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-payment-list',
  templateUrl: './payment-list.component.html',
  styleUrls: ['./payment-list.component.css']
})
export class PaymentListComponent implements OnInit, OnDestroy {
  payments: Payment[] = [];
  loading = true;
  filterStatus = '';

  private subscriptions = new Subscription();

  constructor(private paymentService: PaymentService, private toast: ToastService) {}

  ngOnInit(): void { this.load(); }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  load(): void {
    this.loading = true;
    this.subscriptions.add(this.paymentService.list().subscribe({
      next: (data: Payment[]) => { this.payments = data; this.loading = false; },
      error: (err: any) => { this.toast.error(err.error?.message || 'Erreur'); this.loading = false; }
    }));
  }

  get filteredPayments(): Payment[] {
    if (!this.filterStatus) return this.payments;
    return this.payments.filter(p => p.status === this.filterStatus);
  }

  statusLabel(s: string): string {
    const map: Record<string, string> = {
      PENDING: 'En attente', CONFIRMED: 'Confirmé', REJECTED: 'Rejeté', CANCELLED: 'Annulé'
    };
    return map[s] || s;
  }

  statusClass(s: string): string {
    const map: Record<string, string> = {
      PENDING: 'pending', CONFIRMED: 'confirmed', REJECTED: 'rejected', CANCELLED: 'cancelled'
    };
    return map[s] || '';
  }

  confirm(id: number): void {
    this.subscriptions.add(this.paymentService.confirm(id).subscribe({
      next: () => this.load(),
      error: (e: any) => { this.toast.error(e.error?.message || 'Erreur'); }
    }));
  }

  cancel(id: number): void {
    this.subscriptions.add(this.paymentService.cancel(id).subscribe({
      next: () => this.load(),
      error: (e: any) => { this.toast.error(e.error?.message || 'Erreur'); }
    }));
  }
}
