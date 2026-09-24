import { Component, OnInit } from '@angular/core';
import { BalanceService } from '../../services/balance.service';
import { BalanceSummary, BalanceEntry } from '../../models/balance.model';
import { ToastService } from '../../services/toast.service';
import { LoginService } from '../../services/login.service';

@Component({
    selector: 'app-supplier-balance',
    templateUrl: './supplier-balance.component.html',
    styleUrls: ['./supplier-balance.component.css'],
    standalone: false
})
export class SupplierBalanceComponent implements OnInit {
  balances: BalanceSummary[] = [];
  loading = true;
  supplierId = '';

  selectedShopId: string | null = null;
  ledgerEntries: BalanceEntry[] = [];
  loadingLedger = false;

  creditShopId = '';
  creditAmount: number | null = null;
  creditReason = '';
  submitting = false;

  constructor(
    private balanceService: BalanceService,
    private loginService: LoginService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    const user = this.loginService.getCurrentUser();
    this.supplierId = user?.organizationId || '';
    this.load();
  }

  load(): void {
    if (!this.supplierId) {
      this.loading = false;
      return;
    }
    this.loading = true;
    this.balanceService.listBySupplier(this.supplierId).subscribe({
      next: (data: BalanceSummary[]) => { this.balances = data; this.loading = false; },
      error: (err: any) => { this.toast.error(err.error?.message || 'Erreur'); this.loading = false; }
    });
  }

  viewLedger(supplierId: string, shopId: string): void {
    this.selectedShopId = shopId;
    this.loadingLedger = true;
    this.balanceService.getHistory(supplierId, shopId).subscribe({
      next: (data: BalanceEntry[]) => { this.ledgerEntries = data; this.loadingLedger = false; },
      error: (err: any) => { this.toast.error(err.error?.message || 'Erreur'); this.loadingLedger = false; }
    });
  }

  closeLedger(): void {
    this.selectedShopId = null;
    this.ledgerEntries = [];
  }

  canCredit(): boolean {
    return !!this.creditShopId && !!this.creditAmount && this.creditAmount > 0 && !this.submitting;
  }

  submitCredit(): void {
    if (!this.canCredit()) return;
    this.submitting = true;
    this.balanceService.adjust({
      supplierId: this.supplierId,
      shopId: this.creditShopId,
      amount: this.creditAmount!,
      reason: this.creditReason || 'Crédit manuel fournisseur'
    }).subscribe({
      next: () => {
        this.toast.success('Crédit ajouté avec succès');
        this.creditShopId = '';
        this.creditAmount = null;
        this.creditReason = '';
        this.submitting = false;
        this.load();
      },
      error: (err: any) => {
        this.toast.error(err.error?.message || 'Erreur lors du crédit');
        this.submitting = false;
      }
    });
  }

  typeLabel(t: string): string {
    const map: Record<string, string> = {
      ORDER_CREDIT: 'Commande', PAYMENT_DEBIT: 'Paiement', ADJUSTMENT: 'Ajustement', REFUND: 'Remboursement',
      ORDER: 'Commande', PAYMENT: 'Paiement'
    };
    return map[t] || t;
  }

  typeClass(t: string): string {
    const map: Record<string, string> = {
      ORDER_CREDIT: 'order', PAYMENT_DEBIT: 'payment', ADJUSTMENT: 'adjustment', REFUND: 'refund',
      ORDER: 'order', PAYMENT: 'payment'
    };
    return map[t] || '';
  }

  getTotalBalance(): number {
    return this.balances.reduce((sum, b) => sum + b.currentBalance, 0);
  }
}
