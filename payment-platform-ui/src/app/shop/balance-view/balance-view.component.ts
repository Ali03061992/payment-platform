import { Component, OnInit } from '@angular/core';
import { BalanceService } from '../../services/balance.service';
import { BalanceSummary, BalanceEntry } from '../../models/balance.model';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-balance-view',
  templateUrl: './balance-view.component.html',
  styleUrls: ['./balance-view.component.css']
})
export class BalanceViewComponent implements OnInit {
  balances: BalanceSummary[] = [];
  loading = true;

  selectedSupplierId: number | null = null;
  selectedShopId: number | null = null;
  ledgerEntries: BalanceEntry[] = [];
  loadingLedger = false;

  constructor(private balanceService: BalanceService, private toast: ToastService) {}

  ngOnInit(): void { this.load(); }

  private getShopId(): number {
    const userJson = localStorage.getItem('user');
    if (userJson) {
      const user = JSON.parse(userJson);
      return user.organizationId || 0;
    }
    return 0;
  }

  load(): void {
    this.loading = true;
    const shopId = this.getShopId();
    this.balanceService.listByShop(shopId).subscribe({
      next: (data: BalanceSummary[]) => { this.balances = data; this.loading = false; },
      error: (err: any) => { this.toast.error(err.error?.message || 'Erreur'); this.loading = false; }
    });
  }

  viewLedger(supplierId: number, shopId: number): void {
    this.selectedSupplierId = supplierId;
    this.selectedShopId = shopId;
    this.loadingLedger = true;
    this.balanceService.getHistory(supplierId, shopId).subscribe({
      next: (data: BalanceEntry[]) => { this.ledgerEntries = data; this.loadingLedger = false; },
      error: (err: any) => { this.toast.error(err.error?.message || 'Erreur'); this.loadingLedger = false; }
    });
  }

  closeLedger(): void {
    this.selectedSupplierId = null;
    this.selectedShopId = null;
    this.ledgerEntries = [];
  }

  typeLabel(t: string): string {
    const map: Record<string, string> = {
      ORDER: 'Commande', PAYMENT: 'Paiement', ADJUSTMENT: 'Ajustement'
    };
    return map[t] || t;
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
