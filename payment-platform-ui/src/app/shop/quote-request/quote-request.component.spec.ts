// @ts-nocheck
/**
 * Tests QuoteRequestComponent : demande de devis boutique, zéro remise.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { QuoteRequestComponent } from './quote-request.component';
import { QuoteService } from '../../services/quote.service';
import { OrganizationService } from '../../services/organization.service';
import { StockService } from '../../services/stock.service';
import { ToastService } from '../../services/toast.service';
import { translateServiceProvider, TranslateStubPipe } from '../../testing/translate-stubs';

describe('QuoteRequestComponent', () => {
  let component: QuoteRequestComponent;
  let fixture: ComponentFixture<QuoteRequestComponent>;
  let quoteService: jasmine.SpyObj<QuoteService>;
  let orgService: jasmine.SpyObj<OrganizationService>;
  let stockService: jasmine.SpyObj<StockService>;
  let router: jasmine.SpyObj<Router>;
  let toast: jasmine.SpyObj<ToastService>;

  beforeEach(() => {
    sessionStorage.setItem('user', JSON.stringify({ organizationId: 'shop-1' }));
    const quoteSpy = jasmine.createSpyObj('QuoteService', ['createRequest']);
    const orgSpy = jasmine.createSpyObj('OrganizationService', ['listRelationsByShop', 'listSuppliers']);
    const stockSpy = jasmine.createSpyObj('StockService', ['getProductsBySupplier']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    orgSpy.listRelationsByShop.and.returnValue(of([]));

    TestBed.configureTestingModule({
      declarations: [QuoteRequestComponent, TranslateStubPipe],
      schemas: [NO_ERRORS_SCHEMA],
      imports: [],
      providers: [
        translateServiceProvider(),
        { provide: QuoteService, useValue: quoteSpy },
        { provide: OrganizationService, useValue: orgSpy },
        { provide: StockService, useValue: stockSpy },
        { provide: ToastService, useValue: toastSpy },
        { provide: Router, useValue: routerSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    fixture = TestBed.createComponent(QuoteRequestComponent);
    component = fixture.componentInstance;
    quoteService = TestBed.inject(QuoteService) as jasmine.SpyObj<QuoteService>;
    orgService = TestBed.inject(OrganizationService) as jasmine.SpyObj<OrganizationService>;
    stockService = TestBed.inject(StockService) as jasmine.SpyObj<StockService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  afterEach(() => sessionStorage.clear());

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should add product and increase quantity', () => {
    const p = { id: 'p1', name: 'Widget', sku: 'W-1', unitPrice: 100, quantity: 10, reservedQty: 0 } as any;
    component.addProduct(p);
    component.addProduct(p);
    expect(component.lines.length).toBe(1);
    expect(component.lines[0].quantity).toBe(2);
  });

  it('should not add out-of-stock product', () => {
    component.addProduct({ id: 'p1', quantity: 0, reservedQty: 0 } as any);
    expect(component.lines.length).toBe(0);
  });

  it('should submit demande (zéro remise) and navigate to mes devis', () => {
    quoteService.createRequest.and.returnValue(of({} as any));
    component.selectedSupplierId = 'sup-1';
    component.lines = [{ product: { id: 'p1', unitPrice: 100 } as any, quantity: 2 }];
    component.notes = 'Urgent';
    component.submit();
    expect(quoteService.createRequest).toHaveBeenCalledWith('sup-1', 'Urgent',
      [{ productId: 'p1', quantity: 2 }]);
    expect(toast.success).toHaveBeenCalledWith('QUOTE_REQ.SENT');
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard/shop/quotes']);
  });

  it('should toast on submit error', () => {
    quoteService.createRequest.and.returnValue(throwError(() => ({ error: {} })));
    component.selectedSupplierId = 'sup-1';
    component.lines = [{ product: { id: 'p1' } as any, quantity: 1 }];
    component.submit();
    expect(toast.error).toHaveBeenCalledWith('QUOTE_REQ.CREATE_ERROR');
    expect(component.creating).toBeFalse();
  });
});
