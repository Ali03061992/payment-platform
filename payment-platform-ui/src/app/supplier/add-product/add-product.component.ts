import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { StockService } from '../../services/stock.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-add-product',
  templateUrl: './add-product.component.html',
  styleUrls: ['./add-product.component.css']
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

  constructor(private stockService: StockService, private router: Router, private toast: ToastService) {}

  getCurrencySymbol(code: string): string {
    const symbols: Record<string, string> = { TND: 'DT', EUR: '€', USD: '$' };
    return symbols[code] || code;
  }

  validate(): boolean {
    this.errors = {};

    if (!this.form.name.trim()) {
      this.errors['name'] = 'Le nom est obligatoire.';
    } else if (this.form.name.length < 2) {
      this.errors['name'] = 'Le nom doit contenir au moins 2 caractères.';
    }

    if (!this.form.sku.trim()) {
      this.errors['sku'] = 'Le SKU est obligatoire.';
    } else if (this.form.sku.length < 2) {
      this.errors['sku'] = 'Le SKU doit contenir au moins 2 caractères.';
    }

    if (!this.form.unitPrice || this.form.unitPrice <= 0) {
      this.errors['unitPrice'] = 'Le prix doit être supérieur à 0.';
    }

    if (this.form.quantity < 0) {
      this.errors['quantity'] = 'La quantité ne peut pas être négative.';
    }

    if (this.form.minQuantity < 0) {
      this.errors['minQuantity'] = 'Le seuil ne peut pas être négatif.';
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
        this.toast.success('Produit créé avec succès');
      },
      error: (err) => {
        this.toast.error(err.error?.message || 'Erreur lors de la création');
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
