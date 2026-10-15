import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { QuoteService } from '../../services/quote.service';
import { ToastService } from '../../services/toast.service';
import { ConfirmDialogService } from '../../components/confirm-dialog/confirm-dialog.service';
import { QuoteRequest, SupplierQuote } from '../../models/order.model';

/**
 * Mes devis (boutique) : demandes envoyées + devis reçus, acceptation
 * (« J'accepte ce devis » crée la commande), téléchargement, annulation.
 */
@Component({
    selector: 'app-shop-quotes',
    templateUrl: './shop-quotes.component.html',
    styleUrls: ['./shop-quotes.component.css'],
    standalone: false
})
export class ShopQuotesComponent implements OnInit, OnDestroy {
  demandes: QuoteRequest[] = [];
  loading = true;
  acceptingId: string | null = null;

  private subscriptions = new Subscription();

  constructor(
    private quoteService: QuoteService,
    private router: Router,
    private toast: ToastService,
    private confirmDialog: ConfirmDialogService,
    private translate: TranslateService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  load(): void {
    this.loading = true;
    this.subscriptions.add(this.quoteService.myRequests().subscribe({
      next: (list) => {
        this.demandes = list || [];
        this.loading = false;
      },
      error: (err: { error?: { message?: string } }) => {
        this.toast.error(err.error?.message || this.translate.instant('MY_QUOTES.LOAD_ERROR'));
        this.loading = false;
      }
    }));
  }

  linesOf(demande: QuoteRequest): any[] {
    return this.quoteService.parseLines(demande.linesJson);
  }

  quoteLinesOf(quote: SupplierQuote): any[] {
    return this.quoteService.parseLines(quote.linesJson);
  }

  canAccept(demande: QuoteRequest): boolean {
    return demande.status === 'QUOTED' && (demande.quotes || []).length > 0;
  }

  /** Tous les devis reçus, à plat pour le tableau (avec leur demande). */
  get allQuotes(): { demande: QuoteRequest; quote: SupplierQuote }[] {
    const rows: { demande: QuoteRequest; quote: SupplierQuote }[] = [];
    for (const d of this.demandes) {
      for (const q of d.quotes || []) {
        rows.push({ demande: d, quote: q });
      }
    }
    return rows.sort((a, b) => (b.quote.createdAt || '').localeCompare(a.quote.createdAt || ''));
  }

  latestQuote(demande: QuoteRequest): SupplierQuote | null {
    const qs = demande.quotes || [];
    return qs.length > 0 ? qs[qs.length - 1] : null;
  }

  accept(quote: SupplierQuote): void {
    this.subscriptions.add(this.confirmDialog.confirm({
      title: this.translate.instant('MY_QUOTES.ACCEPT_TITLE'),
      message: this.translate.instant('MY_QUOTES.ACCEPT_MSG', { version: quote.version, total: quote.total })
    }).subscribe((ok: boolean) => {
      if (!ok) return;
      this.acceptingId = quote.id;
      this.subscriptions.add(this.quoteService.acceptQuote(quote.id).subscribe({
        next: (order) => {
          this.acceptingId = null;
          this.toast.success(this.translate.instant('MY_QUOTES.ACCEPTED', { ref: order.reference }));
          this.router.navigate(['/dashboard/shop/orders', order.id]);
        },
        error: (err: { error?: { message?: string } }) => {
          this.acceptingId = null;
          this.toast.error(err.error?.message || this.translate.instant('MY_QUOTES.ACCEPT_ERROR'));
        }
      }));
    }));
  }

  download(quote: SupplierQuote): void {
    this.quoteService.downloadQuotePdf(quote.requestId, quote.version, quote.requestReference);
  }

  cancel(demande: QuoteRequest): void {
    this.subscriptions.add(this.confirmDialog.confirm({
      title: this.translate.instant('MY_QUOTES.CANCEL_TITLE'),
      message: this.translate.instant('MY_QUOTES.CANCEL_MSG', { ref: demande.reference })
    }).subscribe((ok: boolean) => {
      if (!ok) return;
      this.subscriptions.add(this.quoteService.cancelRequest(demande.id).subscribe({
        next: () => {
          this.toast.success(this.translate.instant('MY_QUOTES.CANCELLED'));
          this.load();
        },
        error: (err: { error?: { message?: string } }) => {
          this.toast.error(err.error?.message || this.translate.instant('MY_QUOTES.CANCEL_ERROR'));
        }
      }));
    }));
  }

  newRequest(): void {
    this.router.navigate(['/dashboard/shop/quotes/request']);
  }
}
