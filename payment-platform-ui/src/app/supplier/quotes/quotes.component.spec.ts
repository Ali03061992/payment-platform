// @ts-nocheck
/**
 * Tests QuotesComponent : demandes à chiffrer + envoi via l'application.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { QuotesComponent } from './quotes.component';
import { QuoteService } from '../../services/quote.service';
import { ToastService } from '../../services/toast.service';
import { translateServiceProvider, TranslateStubPipe } from '../../testing/translate-stubs';

describe('QuotesComponent', () => {
  let component: QuotesComponent;
  let fixture: ComponentFixture<QuotesComponent>;
  let quoteService: jasmine.SpyObj<QuoteService>;

  const demande = {
    id: 'd1', reference: 'DEV-001', status: 'PENDING', shopName: 'Shop',
    createdAt: '', notes: '', linesJson: JSON.stringify([{ productId: 'p1', name: 'Widget', qty: 2 }]),
    quotes: []
  } as any;

  beforeEach(() => {
    const quoteSpy = jasmine.createSpyObj('QuoteService', ['myRequests', 'sendQuote', 'downloadQuotePdf', 'parseLines']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    quoteSpy.myRequests.and.returnValue(of([]));
    quoteSpy.parseLines.and.callFake((s: string) => { try { return JSON.parse(s); } catch { return []; } });

    TestBed.configureTestingModule({
      declarations: [QuotesComponent, TranslateStubPipe],
      schemas: [NO_ERRORS_SCHEMA],
      imports: [],
      providers: [
        translateServiceProvider(),
        { provide: QuoteService, useValue: quoteSpy },
        { provide: ToastService, useValue: toastSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    fixture = TestBed.createComponent(QuotesComponent);
    component = fixture.componentInstance;
    quoteService = TestBed.inject(QuoteService) as jasmine.SpyObj<QuoteService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load pending and quoted demandes only', () => {
    quoteService.myRequests.and.returnValue(of([
      { ...demande, id: '1', status: 'PENDING' },
      { ...demande, id: '2', status: 'QUOTED' },
      { ...demande, id: '3', status: 'ACCEPTED' },
      { ...demande, id: '4', status: 'CANCELLED' }
    ] as any));
    component.ngOnInit();
    expect(component.demandes.map(d => d.id)).toEqual(['1', '2']);
    expect(component.loading).toBeFalse();
  });

  it('should open quote modal from demande lines', () => {
    component.openQuote(demande);
    expect(component.showQuoteModal).toBeTrue();
    expect(component.quoteRequestId).toBe('d1');
    expect(component.quoteLines.length).toBe(1);
    expect(component.quoteLines[0].productId).toBe('p1');
    expect(component.quoteLines[0].quantity).toBe(2);
  });

  it('should send quote and reload', () => {
    quoteService.sendQuote.and.returnValue(of({ version: 1 } as any));
    component.quoteRequestId = 'd1';
    component.quoteLines = [{ productId: 'p1', productName: 'Widget', quantity: 2, discount: 10 }];
    component.quoteGlobalDiscount = 5;
    component.submitQuote();
    expect(quoteService.sendQuote).toHaveBeenCalledWith('d1',
      [{ productId: 'p1', quantity: 2, discount: 10 }], 5);
    expect(component.showQuoteModal).toBeFalse();
  });

  it('should reject invalid discounts', () => {
    component.quoteRequestId = 'd1';
    component.quoteLines = [{ productId: 'p1', productName: 'W', quantity: 1, discount: 120 }];
    component.quoteGlobalDiscount = 0;
    expect(component.canSubmitQuote()).toBeFalse();
    component.submitQuote();
    expect(quoteService.sendQuote).not.toHaveBeenCalled();
  });

  it('should download quote pdf', () => {
    component.download('d1', 2, 'DEV-001');
    expect(quoteService.downloadQuotePdf).toHaveBeenCalledWith('d1', 2, 'DEV-001');
  });
});
