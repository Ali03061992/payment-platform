import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { PaymentService } from '../../services/payment.service';
import { OrganizationService } from '../../services/organization.service';
import { Organization } from '../../models/organization.model';
import { ToastService } from '../../services/toast.service';

@Component({
    selector: 'app-create-payment',
    templateUrl: './create-payment.component.html',
    styleUrls: ['./create-payment.component.css'],
    standalone: false
})
export class CreatePaymentComponent implements OnInit {
  shopId = '';
  supplierId = '';
  amount = 0;
  currency = 'TND';
  shops: Organization[] = [];
  suppliers: Organization[] = [];
  creating = false;

  constructor(
    private paymentService: PaymentService,
    private orgService: OrganizationService,
    private router: Router,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    const userJson = sessionStorage.getItem('user');
    if (userJson) {
      const user = JSON.parse(userJson);
      const roles: string[] = user.roles || [];
      if (roles.includes('SHOP_ADMIN') || roles.includes('SHOP_MANAGER')) {
        const shopId = user.organizationId;
        if (shopId) {
          this.shopId = shopId;
          this.shops = [{ id: shopId, name: user.username, type: 'SHOP', status: 'ACTIVE', createdAt: '', updatedAt: '' } as Organization];
          this.loadSuppliersForShop(shopId);
        }
      } else if (roles.includes('SYSTEM_ADMIN')) {
        this.orgService.listShops().subscribe({ next: (d: Organization[]) => this.shops = d });
        this.orgService.listSuppliers().subscribe({ next: (d: Organization[]) => this.suppliers = d });
      }
    }
  }

  private loadSuppliersForShop(shopId: string): void {
    this.orgService.listRelationsByShop(shopId).subscribe({
      next: (relations) => {
        const supplierIds = [...new Set(relations.filter(r => r.status === 'ACTIVE').map(r => r.supplierId))];
        supplierIds.forEach(id => {
          this.orgService.getById(id).subscribe({
            next: (supplier) => this.suppliers.push(supplier)
          });
        });
      }
    });
  }

  create(): void {
    if (!this.shopId || !this.supplierId || this.amount < 0.01 || this.amount > 999999.99) return;
    this.creating = true;
    this.paymentService.create({ shopId: this.shopId, supplierId: this.supplierId, amount: this.amount, currency: this.currency }).subscribe({
      next: (payment) => this.router.navigate(['/dashboard/payments', payment.id]),
      error: (err: any) => {
        this.toast.error(err.error?.message || 'Erreur lors de la création');
        this.creating = false;
      }
    });
  }
}
