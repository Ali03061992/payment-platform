import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { StockService } from '../../services/stock.service';
import { ToastService } from '../../services/toast.service';

@Component({
    selector: 'app-add-product',
    templateUrl: './add-product.component.html',
    styleUrls: ['./add-product.component.css'],
    standalone: false
})
export class AddProductComponent {
  form = {
    name: '',
    sku: '',
    description: '',
    unitPrice: 0,
    currency: 'TND',
    quantity: 0,
    minQuantity: 0
  };

  errors: { [key: string]: string } = {};
  success = false;
  loading = false;

  constructor(private stockService: StockService, private router: Router, private toast: ToastService, private translate: TranslateService) {}

  getCurrencySymbol(code: string): string {
    const symbols: Record<string, string> = { TND: 'DT', EUR: '€', USD: '$' };
    return symbols[code] || code;
  }

  validate(): boolean {
    this.errors = {};

    if (!this.form.name.trim()) {
      this.errors['name'] = 'CATALOG.ERR_NAME_REQUIRED';
    } else if (this.form.name.length < 2) {
      this.errors['name'] = 'CATALOG.ERR_NAME_MIN';
    }

    if (!this.form.sku.trim()) {
      this.errors['sku'] = 'CATALOG.ERR_SKU_REQUIRED';
    } else if (this.form.sku.length < 2) {
      this.errors['sku'] = 'CATALOG.ERR_SKU_MIN';
    }

    if (!this.form.unitPrice || this.form.unitPrice <= 0) {
      this.errors['unitPrice'] = 'CATALOG.ERR_PRICE';
    }

    if (this.form.quantity < 0) {
      this.errors['quantity'] = 'CATALOG.ERR_QTY';
    }

    if (this.form.minQuantity < 0) {
      this.errors['minQuantity'] = 'CATALOG.ERR_MIN';
    }

    return Object.keys(this.errors).length === 0;
  }

  onSubmit(): void {
    if (!this.validate()) return;

    this.loading = true;
    this.stockService.createProduct(this.form).subscribe({
      next: () => {
        this.success = true;
        this.loading = false;
        this.toast.success(this.translate.instant('CATALOG.SUCCESS_CREATED'));
      },
      error: (err) => {
        this.toast.error(err.error?.message || this.translate.instant('CATALOG.CREATE_ERROR'));
        this.loading = false;
      }
    });
  }

  resetForm(): void {
    this.form = { name: '', sku: '', description: '', unitPrice: 0, currency: 'TND', quantity: 0, minQuantity: 0 };
    this.errors = {};
    this.success = false;
  }

  goBack(): void {
    this.router.navigate(['/supplier/stock']);
  }
}
