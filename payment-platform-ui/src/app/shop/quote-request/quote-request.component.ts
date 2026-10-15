import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { QuoteService } from '../../services/quote.service';
import { OrganizationService } from '../../services/organization.service';
import { StockService } from '../../services/stock.service';
import { Organization } from '../../models/organization.model';
import { Product } from '../../models/stock.model';
import { ToastService } from '../../services/toast.service';
import { previewFiscal } from '../../utils/tunisian-fiscal';

interface QuoteLine {
  product: Product;
  quantity: number;
}

/**
 * Demande de devis boutique (interface dédiée) : la boutique décrit son besoin
 * SANS aucune remise ; le fournisseur chiffre ensuite via l'application.
 */
@Component({
    selector: 'app-quote-request',
    templateUrl: './quote-request.component.html',
    styleUrls: ['./quote-request.component.css'],
    standalone: false
})
export class QuoteRequestComponent implements OnInit {
  suppliers: Organization[] = [];
  selectedSupplierId = '';
  products: Product[] = [];
  searchQuery = '';
  lines: QuoteLine[] = [];
  notes = '';
  creating = false;
  shopId = '';
  loadingSuppliers = false;
  loadingProducts = false;
  errorSuppliers: string | null = null;
  errorProducts: string | null = null;

  constructor(
    private quoteService: QuoteService,
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
      this.errorSuppliers = this.translate.instant('QUOTE_REQ.SHOP_NOT_IDENTIFIED');
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
            this.errorSuppliers = this.translate.instant('QUOTE_REQ.LOAD_SUPPLIERS_ERROR');
          }
        });
      },
      error: (err) => {
        this.loadingSuppliers = false;
        this.errorSuppliers = this.translate.instant('QUOTE_REQ.LOAD_RELATIONS_ERROR');
      }
    });
  }

  private getShopId(): string {
    try {
      const userJson = sessionStorage.getItem('user');
      if (userJson) {
        const user = JSON.parse(userJson);
        return user.organizationId || '';
      }
    } catch {}
    return '';
  }

  onSupplierChange(): void {
    this.products = [];
    this.lines = [];
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
        this.errorProducts = this.translate.instant('QUOTE_REQ.LOAD_PRODUCTS_ERROR');
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

  addProduct(product: Product): void {
    if (!this.hasStock(product)) return;
    const existing = this.lines.find(l => l.product.id === product.id);
    if (existing) {
      existing.quantity++;
    } else {
      this.lines.push({ product, quantity: 1 });
    }
  }

  removeLine(index: number): void {
    this.lines.splice(index, 1);
  }

  private get preview() {
    return previewFiscal(
      this.lines.map(l => ({
        unitPrice: l.product.unitPrice,
        quantity: l.quantity,
        discountPct: 0,
        vatRate: l.product.vatRate,
        fodecApplicable: l.product.fodecApplicable
      })),
      0
    );
  }

  get estimatedTotal(): number {
    return this.preview.ttc;
  }

  canSubmit(): boolean {
    return !!this.selectedSupplierId && this.lines.length > 0 && !this.creating;
  }

  submit(): void {
    if (!this.canSubmit()) return;
    this.creating = true;
    this.quoteService.createRequest(
      this.selectedSupplierId,
      this.notes,
      this.lines.map(l => ({ productId: l.product.id, quantity: l.quantity }))
    ).subscribe({
      next: () => {
        this.toast.success(this.translate.instant('QUOTE_REQ.SENT'));
        this.router.navigate(['/dashboard/shop/quotes']);
      },
      error: (err: { error?: { message?: string } }) => {
        this.toast.error(err.error?.message || this.translate.instant('QUOTE_REQ.CREATE_ERROR'));
        this.creating = false;
      }
    });
  }
}
