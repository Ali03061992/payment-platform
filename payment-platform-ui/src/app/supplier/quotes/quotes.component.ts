import { Component, OnInit, OnDestroy } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { QuoteService } from '../../services/quote.service';
import { ToastService } from '../../services/toast.service';
import { QuoteRequest } from '../../models/order.model';

/**
 * Devis côté fournisseur (interface dédiée, via l'application) : liste des
 * demandes à chiffrer, édition des lignes + remises, envoi versionné.
 */
@Component({
    selector: 'app-quotes',
    templateUrl: './quotes.component.html',
    styleUrls: ['./quotes.component.css'],
    standalone: false
})
export class QuotesComponent implements OnInit, OnDestroy {
  demandes: QuoteRequest[] = [];
  loading = true;

  showQuoteModal = false;
  quoteRequestId = '';
  quoteReference = '';
  quoteLines: { productId: string; productName: string; quantity: number; discount: number; unitPrice: number }[] = [];
  sendingQuote = false;

  private subscriptions = new Subscription();

  constructor(
    private quoteService: QuoteService,
    private toast: ToastService,
    private translate: TranslateService
  ) {}

  ngOnInit(): void {
    this.loadDemandes();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  loadDemandes(): void {
    this.loading = true;
    this.subscriptions.add(this.quoteService.myRequests().subscribe({
      next: (list) => {
        this.demandes = (list || []).filter(d => d.status === 'PENDING' || d.status === 'QUOTED');
        this.loading = false;
      },
      error: (err: { error?: { message?: string } }) => {
        this.toast.error(err.error?.message || this.translate.instant('QUOTES.LOAD_ERROR'));
        this.loading = false;
      }
    }));
  }

  linesOf(d: QuoteRequest): any[] {
    return this.quoteService.parseLines(d.linesJson);
  }

  quotesOf(d: QuoteRequest): any[] {
    return d.quotes || [];
  }

  openQuote(d: QuoteRequest): void {
    this.quoteRequestId = d.id;
    this.quoteReference = d.reference;
    this.quoteLines = this.linesOf(d).map((l: any) => ({
      productId: l.productId,
      productName: l.name,
      quantity: l.qty || l.quantity || 1,
      discount: 0,
      unitPrice: l.unitPrice || 0
    }));
    this.sendingQuote = false;
    this.showQuoteModal = true;
  }

  closeQuote(): void {
    this.showQuoteModal = false;
    this.quoteRequestId = '';
    this.quoteLines = [];
  }

  canSubmitQuote(): boolean {
    return this.quoteLines.length > 0 && !this.sendingQuote
      && this.quoteLines.every(l => l.quantity >= 1 && l.discount >= 0 && l.discount <= 100);
  }

  submitQuote(): void {
    if (!this.canSubmitQuote()) return;
    this.sendingQuote = true;
    this.subscriptions.add(this.quoteService.sendQuote(
      this.quoteRequestId,
      this.quoteLines.map(l => ({ productId: l.productId, quantity: l.quantity, discount: l.discount }))
    ).subscribe({
      next: (quote) => {
        this.toast.success(this.translate.instant('QUOTES.QUOTE_SENT', { version: quote.version }));
        this.closeQuote();
        this.sendingQuote = false;
        this.loadDemandes();
      },
      error: (err: { error?: { message?: string } }) => {
        this.toast.error(err.error?.message || this.translate.instant('QUOTES.QUOTE_ERROR'));
        this.sendingQuote = false;
      }
    }));
  }

  download(requestId: string, version: number, reference: string): void {
    this.quoteService.downloadQuotePdf(requestId, version, reference);
  }
}
