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
    currency: 'EUR',
    quantity: 0,
    minQuantity: 0
  };

  success = false;
  loading = false;

  constructor(private stockService: StockService, private router: Router, private toast: ToastService) {}

  onSubmit(): void {
    this.loading = true;
    this.stockService.createProduct(this.form).subscribe({
      next: () => { this.success = true; this.loading = false; this.toast.success('Produit créé avec succès'); },
      error: (err) => { this.toast.error(err.error?.message || "Erreur lors de la création"); this.loading = false; }
    });
  }

  goBack(): void {
    this.router.navigate(['/supplier/stock']);
  }
}
