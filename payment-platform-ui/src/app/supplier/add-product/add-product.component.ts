import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { StockService } from '../../services/stock.service';

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

  error = '';
  success = false;
  loading = false;

  constructor(private stockService: StockService, private router: Router) {}

  onSubmit(): void {
    this.error = '';
    this.loading = true;
    this.stockService.createProduct(this.form).subscribe({
      next: () => { this.success = true; this.loading = false; },
      error: (err) => { this.error = err.error?.message || "Erreur lors de la création"; this.loading = false; }
    });
  }

  goBack(): void {
    this.router.navigate(['/supplier/stock']);
  }
}
