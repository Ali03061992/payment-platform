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
  private idempotencyKey: string | null = null;
  private lastPayloadFingerprint: string | null = null;

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
      if (roles.includes('SHOP_ADMIN')) {
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
    if (this.creating) return;
    if (!this.shopId || !this.supplierId || this.amount < 0.01 || this.amount > 999999.99) return;
    const payload = { shopId: this.shopId, supplierId: this.supplierId, amount: this.amount, currency: this.currency };
    const fingerprint = JSON.stringify(payload);
    // Une clé par intention de paiement : réutilisée sur retry/double-clic,
    // régénérée si l'utilisateur modifie le formulaire (le backend rejoue sans comparer le payload).
    if (this.idempotencyKey === null || this.lastPayloadFingerprint !== fingerprint) {
      this.idempotencyKey = PaymentService.newIdempotencyKey();
      this.lastPayloadFingerprint = fingerprint;
    }
    this.creating = true;
    this.paymentService.create(payload, this.idempotencyKey).subscribe({
      next: (payment) => {
        this.idempotencyKey = null;
        this.lastPayloadFingerprint = null;
        this.router.navigate(['/dashboard/payments', payment.id]);
      },
      error: (err: any) => {
        this.toast.error(err.error?.message || 'Erreur lors de la création');
        this.creating = false;
      }
    });
  }
}
