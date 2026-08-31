import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { PaymentService } from '../../services/payment.service';
import { OrganizationService } from '../../services/organization.service';
import { Organization } from '../../models/organization.model';

@Component({
  selector: 'app-create-payment',
  templateUrl: './create-payment.component.html',
  styleUrls: ['./create-payment.component.css']
})
export class CreatePaymentComponent implements OnInit {
  shopId = 0;
  supplierId = 0;
  amount = 0;
  currency = 'TND';
  shops: Organization[] = [];
  suppliers: Organization[] = [];
  creating = false;
  errorMsg = '';

  constructor(
    private paymentService: PaymentService,
    private orgService: OrganizationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.orgService.listShops().subscribe({ next: (d: Organization[]) => this.shops = d });
    this.orgService.listSuppliers().subscribe({ next: (d: Organization[]) => this.suppliers = d });
  }

  create(): void {
    if (!this.shopId || !this.supplierId || this.amount <= 0) return;
    this.creating = true;
    this.paymentService.create({ shopId: this.shopId, supplierId: this.supplierId, amount: this.amount, currency: this.currency }).subscribe({
      next: (payment) => this.router.navigate(['/dashboard/payments', payment.id]),
      error: (err: any) => {
        this.errorMsg = err.error?.message || 'Erreur lors de la création';
        this.creating = false;
        setTimeout(() => this.errorMsg = '', 3000);
      }
    });
  }
}
