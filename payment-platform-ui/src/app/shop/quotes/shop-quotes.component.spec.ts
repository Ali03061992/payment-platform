// @ts-nocheck
/**
 * Tests ShopQuotesComponent : mes devis, acceptation, téléchargement, annulation.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ShopQuotesComponent } from './shop-quotes.component';
import { QuoteService } from '../../services/quote.service';
import { ToastService } from '../../services/toast.service';
import { ConfirmDialogService } from '../../components/confirm-dialog/confirm-dialog.service';
import { translateServiceProvider, TranslateStubPipe } from '../../testing/translate-stubs';

describe('ShopQuotesComponent', () => {
  let component: ShopQuotesComponent;
  let fixture: ComponentFixture<ShopQuotesComponent>;
  let quoteService: jasmine.SpyObj<QuoteService>;
  let router: jasmine.SpyObj<Router>;
  let confirmDialog: jasmine.SpyObj<ConfirmDialogService>;

  const demande = {
    id: 'd1', reference: 'DEV-001', status: 'QUOTED', supplierName: 'Sup',
    createdAt: '', notes: '', linesJson: '[]',
    quotes: [{ id: 'q1', version: 1, total: 100, currency: 'TND' }]
  } as any;

  beforeEach(() => {
    const quoteSpy = jasmine.createSpyObj('QuoteService',
      ['myRequests', 'acceptQuote', 'cancelRequest', 'downloadQuotePdf', 'parseLines']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    const confirmSpy = jasmine.createSpyObj('ConfirmDialogService', ['confirm']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    confirmSpy.confirm.and.returnValue(of(true));
    quoteSpy.myRequests.and.returnValue(of([]));
    quoteSpy.parseLines.and.returnValue([]);

    TestBed.configureTestingModule({
      declarations: [ShopQuotesComponent, TranslateStubPipe],
      schemas: [NO_ERRORS_SCHEMA],
      imports: [],
      providers: [
        translateServiceProvider(),
        { provide: QuoteService, useValue: quoteSpy },
        { provide: ToastService, useValue: toastSpy },
        { provide: ConfirmDialogService, useValue: confirmSpy },
        { provide: Router, useValue: routerSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    fixture = TestBed.createComponent(ShopQuotesComponent);
    component = fixture.componentInstance;
    quoteService = TestBed.inject(QuoteService) as jasmine.SpyObj<QuoteService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    confirmDialog = TestBed.inject(ConfirmDialogService) as jasmine.SpyObj<ConfirmDialogService>;
  });

  it('should create and load demandes', () => {
    quoteService.myRequests.and.returnValue(of([demande]));
    component.ngOnInit();
    expect(component.demandes.length).toBe(1);
    expect(component.loading).toBeFalse();
  });

  it('should accept quote and navigate to the created order', () => {
    quoteService.acceptQuote.and.returnValue(of({ id: 'ord-1', reference: 'ORD-001' } as any));
    component.accept({ id: 'q1', version: 2, total: 200 } as any);
    expect(quoteService.acceptQuote).toHaveBeenCalledWith('q1');
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard/shop/orders', 'ord-1']);
  });

  it('should not accept when dialog cancelled', () => {
    confirmDialog.confirm.and.returnValue(of(false));
    component.accept({ id: 'q1' } as any);
    expect(quoteService.acceptQuote).not.toHaveBeenCalled();
  });

  it('should download quote pdf', () => {
    component.download({ requestId: 'd1', version: 1, requestReference: 'DEV-001' } as any);
    expect(quoteService.downloadQuotePdf).toHaveBeenCalledWith('d1', 1, 'DEV-001');
  });

  it('should cancel demande and reload', () => {
    quoteService.cancelRequest.and.returnValue(of({} as any));
    component.cancel(demande);
    expect(quoteService.cancelRequest).toHaveBeenCalledWith('d1');
  });

  it('should return latest quote', () => {
    const d = { quotes: [{ version: 1 }, { version: 3 }, { version: 2 }] } as any;
    expect(component.latestQuote(d).version).toBe(2);
  });

  it('should flatten all quotes with their demande, newest first', () => {
    const d1 = { id: 'd1', reference: 'DEV-1', quotes: [
      { id: 'q1', version: 1, createdAt: '2026-01-01T10:00:00' },
      { id: 'q2', version: 2, createdAt: '2026-01-02T10:00:00' }
    ] } as any;
    const d2 = { id: 'd2', reference: 'DEV-2', quotes: [] } as any;
    component.demandes = [d1, d2];
    const rows = component.allQuotes;
    expect(rows.length).toBe(2);
    expect(rows[0].quote.id).toBe('q2');
    expect(rows[0].demande.reference).toBe('DEV-1');
    expect(rows[1].quote.id).toBe('q1');
  });
});
