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
    styleUrls: ['./create-order.component.css'],
    standalone: false
})
export class SupplierCreateOrderComponent implements OnInit, OnDestroy {
  shops: Organization[] = [];
  selectedShopId = '';
  products: Product[] = [];
  searchQuery = '';
  orderLines: OrderLine[] = [];
  asapPayment = false;
  currency = 'TND';
  notes = '';
  creating = false;
  supplierId = '';

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

  private getSupplierId(): string {
    const userJson = sessionStorage.getItem('user');
    if (userJson) {
      const user = JSON.parse(userJson);
      return user.organizationId || '';
    }
    return '';
  }

  loadingShops = false;
  loadingProducts = false;
  errorShops: string | null = null;
  errorProducts: string | null = null;

  loadShops(): void {
    this.loadingShops = true;
    this.errorShops = null;
    const supplierId = this.supplierId;
    if (!supplierId) {
      this.loadingShops = false;
      this.errorShops = 'Fournisseur non identifié';
      return;
    }
    this.subscriptions.add(this.orgService.listRelationsBySupplier(supplierId).subscribe({
      next: (relations) => {
        const activeRelations = relations.filter(r => r.status === 'ACTIVE');
        if (activeRelations.length === 0) {
          this.shops = [];
          this.loadingShops = false;
          return;
        }
        const shopIds = [...new Set(activeRelations.map(r => r.shopId))];
        this.subscriptions.add(this.orgService.listShops().subscribe({
          next: (all) => {
            this.shops = all.filter(s => shopIds.includes(s.id) && s.status === 'ACTIVE');
            this.loadingShops = false;
          },
          error: () => {
            this.loadingShops = false;
            this.errorShops = 'Impossible de charger les boutiques';
          }
        }));
      },
      error: (err) => {
        this.loadingShops = false;
        if (err?.status === 403) {
          this.errorShops = 'Permission insuffisante pour voir les relations (contactez admin)';
        } else {
          this.errorShops = 'Impossible de charger les relations';
        }
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
    this.loadingProducts = true;
    this.errorProducts = null;
    this.subscriptions.add(this.stockService.getProducts('ACTIVE').subscribe({
      next: (data) => {
        this.products = data.filter(p => p.status === 'ACTIVE');
        this.loadingProducts = false;
      },
      error: () => {
        this.loadingProducts = false;
        this.errorProducts = 'Impossible de charger les produits';
        this.products = [];
      }
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
    return !!this.selectedShopId && this.orderLines.length > 0 && !this.creating;
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
