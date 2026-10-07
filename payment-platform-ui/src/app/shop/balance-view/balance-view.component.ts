import { Component, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { BalanceService } from '../../services/balance.service';
import { BalanceSummary, BalanceEntry } from '../../models/balance.model';
import { ToastService } from '../../services/toast.service';
import { paginateItems, sortItems, toggleSortState, ariaSortFor, sortIndicatorFor, SortState } from '../../models/page.model';

@Component({
    selector: 'app-balance-view',
    templateUrl: './balance-view.component.html',
    styleUrls: ['./balance-view.component.css'],
    standalone: false
})
export class BalanceViewComponent implements OnInit {
  balances: BalanceSummary[] = [];
  loading = true;

  selectedSupplierId: string | null = null;
  selectedShopId: string | null = null;
  ledgerEntries: BalanceEntry[] = [];
  loadingLedger = false;
  currentPage = 0;
  pageSize = 10;
  sort: SortState = { field: null, direction: 'asc' };
  ledgerPage = 0;
  ledgerSize = 10;
  ledgerSort: SortState = { field: null, direction: 'asc' };

  constructor(private balanceService: BalanceService, private toast: ToastService, private translate: TranslateService) {}

  ngOnInit(): void { this.load(); }

  private getShopId(): string {
    const userJson = sessionStorage.getItem('user');
    if (userJson) {
      const user = JSON.parse(userJson);
      return user.organizationId || '';
    }
    return '';
  }

  load(): void {
    this.loading = true;
    const shopId = this.getShopId();
    this.balanceService.listByShop(shopId).subscribe({
      next: (data: BalanceSummary[]) => { this.balances = data; this.currentPage = 0; this.loading = false; },
      error: (err: { error?: { message?: string }; status?: number; statusText?: string; message?: string }) => { this.toast.error(err.error?.message || this.translate.instant('SHOP_BALANCE.ERROR')); this.loading = false; }
    });
  }

  /** Tri côté client (endpoint soldes non trié côté serveur). */
  onSort(field: string): void {
    this.sort = toggleSortState(this.sort, field);
    this.currentPage = 0;
  }

  ariaSort(field: string): 'ascending' | 'descending' | 'none' {
    return ariaSortFor(field, this.sort);
  }

  sortIndicator(field: string): string {
    return sortIndicatorFor(field, this.sort);
  }

  get sortedBalances(): BalanceSummary[] {
    return sortItems(this.balances, this.sort.field, this.sort.direction);
  }

  get pagedBalances(): BalanceSummary[] {
    return paginateItems(this.sortedBalances, this.currentPage, this.pageSize);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
  }

  onSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
  }

  onLedgerSort(field: string): void {
    this.ledgerSort = toggleSortState(this.ledgerSort, field);
    this.ledgerPage = 0;
  }

  ledgerAriaSort(field: string): 'ascending' | 'descending' | 'none' {
    return ariaSortFor(field, this.ledgerSort);
  }

  ledgerSortIndicator(field: string): string {
    return sortIndicatorFor(field, this.ledgerSort);
  }

  get sortedLedger(): BalanceEntry[] {
    return sortItems(this.ledgerEntries, this.ledgerSort.field, this.ledgerSort.direction);
  }

  get pagedLedger(): BalanceEntry[] {
    return paginateItems(this.sortedLedger, this.ledgerPage, this.ledgerSize);
  }

  onLedgerPage(page: number): void {
    this.ledgerPage = page;
  }

  onLedgerSize(size: number): void {
    this.ledgerSize = size;
    this.ledgerPage = 0;
  }

  viewLedger(supplierId: string, shopId: string): void {
    this.selectedSupplierId = supplierId;
    this.selectedShopId = shopId;
    this.loadingLedger = true;
    this.ledgerPage = 0;
    this.balanceService.getHistory(supplierId, shopId).subscribe({
      next: (data: BalanceEntry[]) => { this.ledgerEntries = data; this.loadingLedger = false; },
      error: (err: { error?: { message?: string }; status?: number; statusText?: string; message?: string }) => { this.toast.error(err.error?.message || this.translate.instant('SHOP_BALANCE.ERROR')); this.loadingLedger = false; }
    });
  }

  closeLedger(): void {
    this.selectedSupplierId = null;
    this.selectedShopId = null;
    this.ledgerEntries = [];
  }

  typeLabel(t: string): string {
    const map: Record<string, string> = {
      ORDER: 'SHOP_BALANCE.TYPE_ORDER', PAYMENT: 'SHOP_BALANCE.TYPE_PAYMENT', ADJUSTMENT: 'SHOP_BALANCE.TYPE_ADJUSTMENT'
    };
    return this.translate.instant(map[t] || t);
  }

  typeClass(t: string): string {
    const map: Record<string, string> = {
      ORDER: 'order', PAYMENT: 'payment', ADJUSTMENT: 'adjustment'
    };
    return map[t] || '';
  }

  getTotalRemaining(): number {
    return this.balances.reduce((sum, b) => sum + b.remainingDue, 0);
  }
}
