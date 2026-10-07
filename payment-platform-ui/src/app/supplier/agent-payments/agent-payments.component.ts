import { Component, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { PaymentService } from '../../services/payment.service';
import { LoginService } from '../../services/login.service';
import { AgentPaymentSummary, Payment } from '../../models/agent-payment.model';
import { ToastService } from '../../services/toast.service';
import { paginateItems, sortItems, toggleSortState, ariaSortFor, sortIndicatorFor, SortState } from '../../models/page.model';
import { statusLabelFr } from '../../pipes/status-label.pipe';

@Component({
    selector: 'app-agent-payments',
    templateUrl: './agent-payments.component.html',
    styleUrls: ['./agent-payments.component.css'],
    standalone: false
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
  currentPage = 0;
  pageSize = 10;
  sort: SortState = { field: null, direction: 'asc' };

  constructor(
    private paymentService: PaymentService,
    private loginService: LoginService,
    private toast: ToastService,
    private translate: TranslateService
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
        this.toast.error(this.translate.instant('AGENT_PAYMENTS.LOAD_ERROR'));
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
    return sortItems(payments, this.sort.field, this.sort.direction);
  }

  onSort(field: string): void {
    this.sort = toggleSortState(this.sort, field);
  }

  ariaSort(field: string): 'ascending' | 'descending' | 'none' {
    return ariaSortFor(field, this.sort);
  }

  sortIndicator(field: string): string {
    return sortIndicatorFor(field, this.sort);
  }

  get sortedSummaries(): AgentPaymentSummary[] {
    return sortItems(this.summaries, this.sort.field, this.sort.direction);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
  }

  onSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
  }

  statusLabel(s: string): string {
    return statusLabelFr(s);
  }

  private formatDate(d: Date): string {
    return d.toISOString().split('T')[0];
  }
}