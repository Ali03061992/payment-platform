import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { OrderService } from '../../services/order.service';
import { ToastService } from '../../services/toast.service';
import { InvoiceJournalEntry } from '../../models/order.model';
import { Subscription } from 'rxjs';

@Component({
    selector: 'app-invoices-journal',
    templateUrl: './invoices-journal.component.html',
    styleUrls: ['./invoices-journal.component.css'],
    standalone: false
})
export class InvoicesJournalComponent implements OnInit, OnDestroy {
  entries: InvoiceJournalEntry[] = [];
  loading = true;
  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;
  selectedYear: number | null = null;
  readonly currentYear = new Date().getFullYear();
  readonly availableYears: number[];

  private subscriptions = new Subscription();

  constructor(private orderService: OrderService, private toast: ToastService,
              private translate: TranslateService, private router: Router) {
    this.availableYears = [this.currentYear, this.currentYear - 1, this.currentYear - 2];
  }

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  load(): void {
    this.loading = true;
    this.subscriptions.add(this.orderService
      .getInvoicesJournal(this.currentPage, this.pageSize, this.selectedYear ?? undefined)
      .subscribe({
        next: (page) => {
          this.entries = page.items || [];
          this.totalElements = page.totalElements ?? this.entries.length;
          this.totalPages = page.totalPages ?? 1;
          this.currentPage = page.number ?? this.currentPage;
          this.loading = false;
        },
        error: (err: { error?: { message?: string } }) => {
          this.toast.error(err.error?.message || this.translate.instant('INVOICE_JOURNAL.ERROR'));
          this.loading = false;
        }
      }));
  }

  onYearChange(): void {
    this.currentPage = 0;
    this.load();
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.load();
  }

  onSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.load();
  }

  downloadPdf(entry: InvoiceJournalEntry): void {
    this.orderService.downloadInvoice(entry.orderId);
  }

  openInvoice(entry: InvoiceJournalEntry): void {
    this.router.navigate(['/dashboard/invoices', entry.invoiceNumber]);
  }
}
