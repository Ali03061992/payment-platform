import { Component, OnInit } from '@angular/core';
import { PaymentService } from '../../services/payment.service';
import { PaymentStats } from '../../models/payment.model';

@Component({
    selector: 'app-payment-stats',
    templateUrl: './payment-stats.component.html',
    styleUrls: ['./payment-stats.component.css'],
    standalone: false
})
export class PaymentStatsComponent implements OnInit {
  stats: PaymentStats | null = null;
  loading = true;

  constructor(private paymentService: PaymentService) {}

  ngOnInit(): void {
    this.paymentService.getStats().subscribe({
      next: (data: PaymentStats) => { this.stats = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }
}
