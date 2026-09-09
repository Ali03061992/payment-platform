import { Component, OnInit } from '@angular/core';
import { PaymentService } from '../../services/payment.service';
import { LoginService } from '../../services/login.service';
import { AgentPaymentSummary, Payment } from '../../models/agent-payment.model';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-agent-payments',
  templateUrl: './agent-payments.component.html',
  styleUrls: ['./agent-payments.component.css']
})
export class AgentPaymentsComponent implements OnInit {
  summaries: AgentPaymentSummary[] = [];
  loading = false;

  fromDate = '';
  toDate = '';
  supplierId = '';

  expandedAgent: string | null = null;
  grandTotal = 0;
  grandConfirmedTotal = 0;
  totalPayments = 0;

  statusFilter = '';

  constructor(
    private paymentService: PaymentService,
    private loginService: LoginService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    const user = this.loginService.getCurrentUser();
    this.supplierId = user?.organizationId || '';

    const today = new Date();
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);

    this.fromDate = this.formatDate(firstDay);
    this.toDate = this.formatDate(today);

    this.load();
  }

  load(): void {
    if (!this.supplierId || !this.fromDate || !this.toDate) return;
    this.loading = true;

    this.paymentService.getAgentSummary(this.supplierId, this.fromDate, this.toDate).subscribe({
      next: (data: AgentPaymentSummary[]) => {
        this.summaries = data;
        this.computeTotals();
        this.loading = false;
      },
      error: () => {
        this.toast.error('Erreur lors du chargement');
        this.loading = false;
      }
    });
  }

  computeTotals(): void {
    this.grandTotal = this.summaries.reduce((s, a) => s + Number(a.totalAmount), 0);
    this.grandConfirmedTotal = this.summaries.reduce((s, a) => s + Number(a.confirmedTotal), 0);
    this.totalPayments = this.summaries.reduce((s, a) => s + a.paymentCount, 0);
  }

  toggleAgent(userId: string): void {
    this.expandedAgent = this.expandedAgent === userId ? null : userId;
  }

  getAgentPayments(agent: AgentPaymentSummary): Payment[] {
    let payments = agent.payments;
    if (this.statusFilter) {
      payments = payments.filter((p: Payment) => p.status === this.statusFilter);
    }
    return payments;
  }

  statusLabel(s: string): string {
    const map: Record<string, string> = { PENDING: 'En attente', CONFIRMED: 'Confirmé', REJECTED: 'Rejeté', CANCELLED: 'Annulé' };
    return map[s] || s;
  }

  private formatDate(d: Date): string {
    return d.toISOString().split('T')[0];
  }
}
