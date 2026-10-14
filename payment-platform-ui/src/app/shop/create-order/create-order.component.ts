import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { OrderService } from '../../services/order.service';
import { OrganizationService } from '../../services/organization.service';
import { StockService } from '../../services/stock.service';
import { Organization } from '../../models/organization.model';
import { Product } from '../../models/stock.model';
import { CreateOrderRequest } from '../../models/order.model';
import { ToastService } from '../../services/toast.service';
import { previewFiscal } from '../../utils/tunisian-fiscal';

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

  constructor(
    private orderService: OrderService,
    private orgService: OrganizationService,
    private stockService: StockService,
    private router: Router,
    private toast: ToastService,
    private translate: TranslateService
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
      this.errorSuppliers = this.translate.instant('ORDER_CREATE.SHOP_NOT_IDENTIFIED');
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
            this.errorSuppliers = this.translate.instant('ORDER_CREATE.LOAD_SUPPLIERS_ERROR');
          }
        });
      },
      error: (err) => {
        this.loadingSuppliers = false;
        if (err?.status === 403) {
          this.errorSuppliers = this.translate.instant('ORDER_CREATE.RELATIONS_FORBIDDEN');
        } else {
          this.errorSuppliers = this.translate.instant('ORDER_CREATE.LOAD_RELATIONS_ERROR');
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
        this.errorProducts = this.translate.instant('ORDER_CREATE.LOAD_PRODUCTS_ERROR');
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
      const subject = encodeURIComponent(this.translate.instant('ORDER_CREATE.RESTOCK_SUBJECT', { name: this.selectedSupplier.name }));
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

  /** Aperçu fiscal (TVA par produit 19/13/7, FODEC, timbre) — le backend recalcule.
   * La boutique ne saisit AUCUNE remise (remises = devis fournisseur). */
  private get fiscalPreview() {
    return previewFiscal(
      this.orderLines.map(l => ({
        unitPrice: l.product.unitPrice,
        quantity: l.quantity,
        discountPct: 0,
        vatRate: l.product.vatRate,
        fodecApplicable: l.product.fodecApplicable
      })),
      0
    );
  }

  get subtotal(): number {
    return this.fiscalPreview.subtotal;
  }

  get taxAmount(): number {
    return this.fiscalPreview.vatTotal;
  }

  get discountAmount(): number {
    return this.fiscalPreview.discountAmount;
  }

  get fodecAmount(): number {
    return this.fiscalPreview.fodec;
  }

  get vatByRate(): { rate: number; base: number; amount: number }[] {
    return this.fiscalPreview.vatByRate;
  }

  get stampAmount(): number {
    return this.fiscalPreview.stamp;
  }

  get netPayable(): number {
    return this.fiscalPreview.netPayable;
  }

  get total(): number {
    return this.fiscalPreview.ttc;
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
      globalDiscount: 0,
      notes: this.notes,
      items: this.orderLines.map(l => ({
        productId: l.product.id,
        quantity: l.quantity,
        discount: 0
      }))
    };
    this.orderService.create(request).subscribe({
      next: () => this.router.navigate(['/dashboard/shop/orders']),
      error: (err: { error?: { message?: string }; status?: number; statusText?: string; message?: string }) => {
        this.toast.error(err.error?.message || this.translate.instant('ORDER_CREATE.CREATE_ERROR'));
        this.creating = false;
      }
    });
  }
}
