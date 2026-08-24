import { Component, OnInit } from '@angular/core';
import { PaymentService } from '../../services/payment.service';
import { Payment } from '../../models/payment.model';

@Component({
  selector: 'app-payment-list',
  templateUrl: './payment-list.component.html',
  styleUrls: ['./payment-list.component.css']
})
export class PaymentListComponent implements OnInit {
  payments: Payment[] = [];
  loading = true;
  filterStatus = '';
  errorMsg = '';

  constructor(private paymentService: PaymentService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.paymentService.list().subscribe({
      next: (data: Payment[]) => { this.payments = data; this.loading = false; },
      error: (err: any) => { this.errorMsg = err.error?.message || 'Erreur'; this.loading = false; }
    });
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
    this.paymentService.confirm(id).subscribe({ next: () => this.load(), error: (e: any) => this.errorMsg = e.error?.message });
    setTimeout(() => this.errorMsg = '', 3000);
  }

  cancel(id: number): void {
    this.paymentService.cancel(id).subscribe({ next: () => this.load(), error: (e: any) => this.errorMsg = e.error?.message });
    setTimeout(() => this.errorMsg = '', 3000);
  }
}
