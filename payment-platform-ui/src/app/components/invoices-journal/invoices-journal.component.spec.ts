// @ts-nocheck
/** Entête JDoc obligatoire : Périmètre / Moyens */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { TranslateStubPipe, translateServiceProvider } from '../../testing/translate-stubs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of } from 'rxjs';
import { InvoicesJournalComponent } from './invoices-journal.component';
import { OrderService } from '../../services/order.service';
import { ToastService } from '../../services/toast.service';

describe('InvoicesJournalComponent', () => {
  let component: InvoicesJournalComponent;
  let fixture: ComponentFixture<InvoicesJournalComponent>;
  let orderService: jasmine.SpyObj<OrderService>;
  let toastService: jasmine.SpyObj<ToastService>;

  const samplePage = {
    items: [{
      id: 'j1',
      invoiceNumber: 'FA-2026-000001',
      orderId: 'ord-1',
      orderReference: 'ORD-001',
      supplierId: 'sup-1',
      supplierName: 'Fournisseur Test',
      shopId: 'shop-1',
      shopName: 'Boutique Test',
      totalTtc: 2380,
      vatAmount: 380,
      currency: 'TND',
      issuedAt: '2026-10-19T10:00:00Z'
    }],
    totalElements: 1,
    totalPages: 1,
    number: 0
  };

  beforeEach(async () => {
    orderService = jasmine.createSpyObj('OrderService', ['getInvoicesJournal', 'downloadInvoice']);
    orderService.getInvoicesJournal.and.returnValue(of(samplePage));
    toastService = jasmine.createSpyObj('ToastService', ['error']);

    await TestBed.configureTestingModule({
      declarations: [InvoicesJournalComponent, TranslateStubPipe],
      schemas: [NO_ERRORS_SCHEMA],
      imports: [FormsModule],
      providers: [
        translateServiceProvider(),
        { provide: OrderService, useValue: orderService },
        { provide: ToastService, useValue: toastService },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InvoicesJournalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('ngOnInit charge le journal page par défaut', () => {
    expect(orderService.getInvoicesJournal).toHaveBeenCalledWith(0, 20, undefined);
    expect(component.entries.length).toBe(1);
    expect(component.entries[0].invoiceNumber).toBe('FA-2026-000001');
    expect(component.loading).toBeFalse();
  });

  it('load applique le filtre année et repart à la page 0', () => {
    component.currentPage = 3;
    component.selectedYear = 2025;
    component.onYearChange();
    expect(orderService.getInvoicesJournal).toHaveBeenCalledWith(0, 20, 2025);
    expect(component.currentPage).toBe(0);
  });

  it('onPageChange charge la page demandée', () => {
    orderService.getInvoicesJournal.and.returnValue(of({ items: [], totalElements: 0, totalPages: 1, number: 2 }));
    component.onPageChange(2);
    expect(orderService.getInvoicesJournal).toHaveBeenCalledWith(2, 20, undefined);
    expect(component.currentPage).toBe(2);
  });

  it('onSizeChange réinitialise la page', () => {
    component.onSizeChange(50);
    expect(orderService.getInvoicesJournal).toHaveBeenCalledWith(0, 50, undefined);
    expect(component.pageSize).toBe(50);
  });

  it('downloadPdf délègue au service avec orderId', () => {
    component.downloadPdf(component.entries[0]);
    expect(orderService.downloadInvoice).toHaveBeenCalledWith('ord-1');
  });

  it('erreur serveur => toast + loading false', () => {
    orderService.getInvoicesJournal.and.returnValue({ subscribe: (obs: any) => obs.error({ error: { message: 'boom' } }) } as any);
    component.load();
    expect(toastService.error).toHaveBeenCalledWith('boom');
    expect(component.loading).toBeFalse();
  });
});
