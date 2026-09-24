import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { OrganizationService } from '../../services/organization.service';
import { StockService } from '../../services/stock.service';
import { Organization } from '../../models/organization.model';
import { Product } from '../../models/stock.model';
import { CreateOrderRequest } from '../../models/order.model';
import { ToastService } from '../../services/toast.service';

interface OrderLine {
  product: Product;
  quantity: number;
  discount: number;
}

@Component({
    selector: 'app-create-order',
    templateUrl: './create-order.component.html',
    styleUrls: ['./create-order.component.css'],
    standalone: false
})
export class CreateOrderComponent implements OnInit {
  suppliers: Organization[] = [];
  selectedSupplierId = '';
  products: Product[] = [];
  searchQuery = '';
  orderLines: OrderLine[] = [];
  asapPayment = false;
  paymentTerms = 'IMMEDIATE';
  currency = 'TND';
  notes = '';
  creating = false;
  shopId = '';
  loadingSuppliers = false;
  loadingProducts = false;
  errorSuppliers: string | null = null;
  errorProducts: string | null = null;
  protected globalDiscount = 0;

  constructor(
    private orderService: OrderService,
    private orgService: OrganizationService,
    private stockService: StockService,
    private router: Router,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.shopId = this.getShopId();
    this.loadSuppliers();
  }

  loadSuppliers(): void {
    this.loadingSuppliers = true;
    this.errorSuppliers = null;
    const shopId = this.shopId;
    if (!shopId) {
      this.loadingSuppliers = false;
      this.errorSuppliers = 'Boutique non identifiée';
      return;
    }
    this.orgService.listRelationsByShop(shopId).subscribe({
      next: (relations) => {
        const activeRelations = relations.filter(r => r.status === 'ACTIVE');
        if (activeRelations.length === 0) {
          this.suppliers = [];
          this.loadingSuppliers = false;
          return;
        }
        const supplierIds = [...new Set(activeRelations.map(r => r.supplierId))];
        this.orgService.listSuppliers().subscribe({
          next: (all) => {
            this.suppliers = all.filter(s => supplierIds.includes(s.id) && s.status === 'ACTIVE');
            this.loadingSuppliers = false;
          },
          error: () => {
            this.loadingSuppliers = false;
            this.errorSuppliers = 'Impossible de charger les fournisseurs';
          }
        });
      },
      error: (err) => {
        this.loadingSuppliers = false;
        if (err?.status === 403) {
          this.errorSuppliers = 'Permission insuffisante pour voir les relations (contactez admin)';
        } else {
          this.errorSuppliers = 'Impossible de charger les relations';
        }
      }
    });
  }

  private getShopId(): string {
    const userJson = sessionStorage.getItem('user');
    if (userJson) {
      const user = JSON.parse(userJson);
      return user.organizationId || '';
    }
    return '';
  }

  onSupplierChange(): void {
    this.products = [];
    this.orderLines = [];
    this.searchQuery = '';
    if (this.selectedSupplierId) {
      this.loadProducts();
    }
  }

  loadProducts(): void {
    this.loadingProducts = true;
    this.errorProducts = null;
    this.stockService.getProductsBySupplier(this.selectedSupplierId, 'ACTIVE').subscribe({
      next: (data) => {
        this.products = data.filter(p => p.supplierId === this.selectedSupplierId && p.status === 'ACTIVE');
        this.loadingProducts = false;
      },
      error: () => {
        this.errorProducts = 'Impossible de charger les produits';
        this.loadingProducts = false;
        this.products = [];
      }
    });
  }

  get filteredProducts(): Product[] {
    if (!this.searchQuery) return this.products;
    const q = this.searchQuery.toLowerCase();
    return this.products.filter(p =>
      p.name.toLowerCase().includes(q) || p.sku.toLowerCase().includes(q)
    );
  }

  hasStock(product: Product): boolean {
    return (product.quantity - (product.reservedQty || 0)) >= 1;
  }

  get availableProductsCount(): number {
    return this.products.filter(p => this.hasStock(p)).length;
  }

  get totalProductsCount(): number {
    return this.products.length;
  }

  get allProductsEmpty(): boolean {
    return this.products.length === 0 || this.availableProductsCount === 0;
  }

  get selectedSupplier(): Organization | undefined {
    return this.suppliers.find(s => s.id === this.selectedSupplierId);
  }

  contactSupplier(): void {
    if (this.selectedSupplier) {
      const subject = encodeURIComponent('Demande de réapprovisionnement - ' + this.selectedSupplier.name);
      window.location.href = 'mailto:?subject=' + subject;
    }
  }

  addProduct(product: Product): void {
    if (!this.hasStock(product)) return;
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

  readonly taxRate = 19;

  get discountAmount(): number {
    return (this.subtotal + this.taxAmount) * (this.globalDiscount / 100);
  }

  get total(): number {
    return this.subtotal + this.taxAmount - this.discountAmount;
  }

  canSubmit(): boolean {
    return !!this.selectedSupplierId && this.orderLines.length > 0 && !this.creating;
  }

  submit(): void {
    if (!this.canSubmit()) return;
    this.creating = true;
    const request: CreateOrderRequest = {
      supplierId: this.selectedSupplierId,
      shopId: this.shopId,
      asapPayment: this.asapPayment,
      paymentTerms: this.paymentTerms,
      currency: this.currency,
      globalDiscount: this.globalDiscount,
      notes: this.notes,
      items: this.orderLines.map(l => ({
        productId: l.product.id,
        quantity: l.quantity,
        discount: l.discount
      }))
    };
    this.orderService.create(request).subscribe({
      next: () => this.router.navigate(['/dashboard/shop/orders']),
      error: (err: any) => {
        this.toast.error(err.error?.message || 'Erreur lors de la création');
        this.creating = false;
      }
    });
  }
}
