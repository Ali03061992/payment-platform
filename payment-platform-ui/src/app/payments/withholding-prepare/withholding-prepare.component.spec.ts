import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { WithholdingPrepareComponent } from './withholding-prepare.component';
import { OrderService } from '../../services/order.service';
import { PdfInvoiceTextService } from '../../services/pdf-invoice-text.service';
import { ToastService } from '../../services/toast.service';
import { TranslateStubPipe, translateServiceProvider } from '../../testing/translate-stubs';

describe('WithholdingPrepareComponent', () => {
  let component: WithholdingPrepareComponent;
  let fixture: ComponentFixture<WithholdingPrepareComponent>;
  let orderService: jasmine.SpyObj<OrderService>;
  let pdfText: jasmine.SpyObj<PdfInvoiceTextService>;

  beforeEach(() => {
    const psSpy = jasmine.createSpyObj('OrderService', ['getById', 'getByReference', 'getWithholdingCertificate',
      'downloadWithholdingCertificatePdf', 'downloadWithholdingCertificateXml', 'markCertificateDeposited']);
    const pdfSpy = jasmine.createSpyObj('PdfInvoiceTextService', ['extractText']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    psSpy.getByReference.and.returnValue(of({ id: 'ord-1', status: 'DELIVERED' } as any));
    psSpy.getWithholdingCertificate.and.returnValue(of({ number: 'CRT-1' } as any));

    TestBed.configureTestingModule({
      declarations: [WithholdingPrepareComponent, TranslateStubPipe],
      imports: [FormsModule],
      providers: [
        translateServiceProvider(),
        { provide: OrderService, useValue: psSpy },
        { provide: PdfInvoiceTextService, useValue: pdfSpy },
        { provide: ToastService, useValue: toastSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    fixture = TestBed.createComponent(WithholdingPrepareComponent);
    component = fixture.componentInstance;
    orderService = TestBed.inject(OrderService) as jasmine.SpyObj<OrderService>;
    pdfText = TestBed.inject(PdfInvoiceTextService) as jasmine.SpyObj<PdfInvoiceTextService>;
  });

  it('should create with reference mode by default', () => {
    expect(component).toBeTruthy();
    expect(component.mode).toBe('reference');
  });

  it('should lookup certificate by order reference', () => {
    component.reference = 'ORD-1';
    component.lookupByReference();
    expect(orderService.getByReference).toHaveBeenCalledWith('ORD-1');
    expect(component.certificate).toEqual({ number: 'CRT-1' } as any);
  });

  it('should compute manual withholding preview', () => {
    component.setMode('manual');
    component.form.grossAmount = 10000;
    component.form.nature = 'MARCHANDISES';
    component.form.beneficiaryMatricule = 'MF';
    expect(component.manualComputation.withholding).toBe(150);
    expect(component.canBuildManualXml()).toBeFalse();
    component.form.debtorName = 'D';
    component.form.beneficiaryName = 'B';
    component.form.paymentReference = 'PAY-9';
    component.form.paymentDate = '2026-07-15';
    expect(component.canBuildManualXml()).toBeTrue();
    component.buildManualXml();
    expect(component.manualXml).toContain('<certificat>');
    expect(component.manualXml).toContain('<echeance>2026-08-31</echeance>');
  });

  it('should prefill from PDF text hints', async () => {
    pdfText.extractText.and.returnValue(Promise.resolve('MF: 1234567A/A/M/000 Total 11 901,000 DT'));
    const file = new File(['x'], 'facture.pdf', { type: 'application/pdf' });
    const event = { target: { files: [file] } } as any;
    await component.onPdfSelected(event);
    expect(component.form.beneficiaryMatricule).toBe('1234567A/A/M/000');
    expect(component.form.grossAmount).toBe(11901);
  });

  it('should handle missing certificate gracefully', () => {
    orderService.getWithholdingCertificate.and.returnValue(throwError(() => ({ status: 404 })));
    component.reference = 'PAY-2';
    component.lookupByReference();
    expect(component.certificate).toBeNull();
  });
});
