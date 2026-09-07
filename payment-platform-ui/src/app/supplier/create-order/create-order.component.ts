import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { OrganizationService } from '../../services/organization.service';
import { StockService } from '../../services/stock.service';
import { Organization } from '../../models/organization.model';
import { Product } from '../../models/stock.model';
import { CreateOrderRequest } from '../../models/order.model';
import { ToastService } from '../../services/toast.service';
import { Subscription } from 'rxjs';

interface OrderLine {
  product: Product;
  quantity: number;
  discount: number;
}

@Component({
  selector: 'app-supplier-create-order',
  templateUrl: './create-order.component.html',
  styleUrls: ['./create-order.component.css']
})
export class SupplierCreateOrderComponent implements OnInit, OnDestroy {
  shops: Organization[] = [];
  selectedShopId = 0;
  products: Product[] = [];
  searchQuery = '';
  orderLines: OrderLine[] = [];
  asapPayment = false;
  currency = 'TND';
  notes = '';
  creating = false;
  supplierId = 0;

  constructor(
    private orderService: OrderService,
    private orgService: OrganizationService,
    private stockService: StockService,
    private router: Router,
    private toast: ToastService
  ) {}

  private subscriptions = new Subscription();

  ngOnInit(): void {
    this.supplierId = this.getSupplierId();
    this.loadShops();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  private getSupplierId(): number {
    const userJson = sessionStorage.getItem('user');
    if (userJson) {
      const user = JSON.parse(userJson);
      return user.organizationId || 0;
    }
    return 0;
  }

  loadShops(): void {
    this.subscriptions.add(this.orgService.listRelations().subscribe({
      next: (relations) => {
        const shopIds = [...new Set(
          relations.filter(r => r.supplierId === this.supplierId && r.status === 'ACTIVE').map(r => r.shopId)
        )];
        this.subscriptions.add(this.orgService.listShops().subscribe({
          next: (all) => { this.shops = all.filter(s => shopIds.includes(s.id)); }
        }));
      }
    }));
  }

  onShopChange(): void {
    this.products = [];
    this.orderLines = [];
    this.searchQuery = '';
    if (this.selectedShopId) {
      this.loadProducts();
    }
  }

  loadProducts(): void {
    this.subscriptions.add(this.stockService.getProducts('ACTIVE').subscribe({
      next: (data) => { this.products = data; }
    }));
  }

  get filteredProducts(): Product[] {
    if (!this.searchQuery) return this.products;
    const q = this.searchQuery.toLowerCase();
    return this.products.filter(p =>
      p.name.toLowerCase().includes(q) || p.sku.toLowerCase().includes(q)
    );
  }

  addProduct(product: Product): void {
    const existing = this.orderLines.find(l => l.product.id === product.id);
    if (existing) {
      existing.quantity++;
    } else {
      this.orderLines.push({ product, quantity: 1, discount: 0 });
    }
  }

  removeLine(index: number): void {
    this.orderLines.splice(index, 1);
  }

  get subtotal(): number {
    return this.orderLines.reduce((sum, l) => sum + (l.product.unitPrice * l.quantity * (1 - l.discount / 100)), 0);
  }

  get taxAmount(): number {
    return this.subtotal * 0.19;
  }

  get total(): number {
    return this.subtotal + this.taxAmount;
  }

  canSubmit(): boolean {
    return this.selectedShopId > 0 && this.orderLines.length > 0 && !this.creating;
  }

  submit(): void {
    if (!this.canSubmit()) return;
    this.creating = true;
    const request: CreateOrderRequest = {
      supplierId: this.supplierId,
      shopId: this.selectedShopId,
      asapPayment: this.asapPayment,
      currency: this.currency,
      notes: this.notes,
      items: this.orderLines.map(l => ({
        productId: l.product.id,
        quantity: l.quantity,
        discount: l.discount
      }))
    };
    this.subscriptions.add(this.orderService.create(request).subscribe({
      next: () => {
        this.toast.success('Commande créée avec succès');
        this.router.navigate(['/dashboard/supplier/orders']);
      },
      error: (err: any) => {
        this.toast.error(err.error?.message || 'Erreur lors de la création');
        this.creating = false;
      }
    }));
  }
}
