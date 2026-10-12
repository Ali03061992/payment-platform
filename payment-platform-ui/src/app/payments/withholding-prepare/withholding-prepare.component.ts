import { Component } from '@angular/core';
import { OrderService } from '../../services/order.service';
import { PdfInvoiceTextService } from '../../services/pdf-invoice-text.service';
import { ToastService } from '../../services/toast.service';
import { TranslateService } from '@ngx-translate/core';
import { WithholdingCertificate } from '../../models/order.model';
import { buildTejXml, computeManualWithholding, extractPdfHints, tejDueDate, WITHHOLDING_NATURES } from '../../utils/tej-xml';
import { saveBlob } from '../../core/file-download';

type Mode = 'reference' | 'manual';

/**
 * Prépare un dépôt TEJ depuis l'app : par référence de commande/facture (données
 * existantes) ou en saisie libre (avec import PDF assisté). Génère le XML prêt
 * à déposer + le marquage de la référence TEJ.
 */
@Component({
    selector: 'app-withholding-prepare',
    templateUrl: './withholding-prepare.component.html',
    styleUrls: ['./withholding-prepare.component.css'],
    standalone: false
})
export class WithholdingPrepareComponent {
  mode: Mode = 'reference';
  natures = WITHHOLDING_NATURES;

  // Mode référence
  reference = '';
  loadingRef = false;
  certificate: WithholdingCertificate | null = null;
  refTejReference = '';
  savingDeposit = false;

  // Mode saisie libre
  form = {
    number: '', debtorName: '', debtorMatricule: '', debtorAddress: '',
    beneficiaryName: '', beneficiaryMatricule: '', beneficiaryAddress: '',
    grossAmount: 0, nature: 'MARCHANDISES', paymentReference: '',
    paymentDate: '', billingPeriod: '', depositorTejId: ''
  };
  pdfFileName = '';
  pdfParsing = false;
  pdfHints: string[] = [];
  manualXml: string | null = null;

  constructor(
    private orderService: OrderService,
    private pdfText: PdfInvoiceTextService,
    private toast: ToastService,
    private translate: TranslateService
  ) {}

  setMode(mode: Mode): void {
    this.mode = mode;
  }

  // ── Par référence de commande/facture ──────────────────────────
  lookupByReference(): void {
    const ref = this.reference.trim();
    if (!ref) return;
    this.loadingRef = true;
    this.certificate = null;
    const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(ref);
    const order$ = isUuid
      ? this.orderService.getById(ref)
      : this.orderService.getByReference(ref);
    order$.subscribe({
      next: (order) => {
        this.orderService.getWithholdingCertificate(order.id).subscribe({
          next: (cert) => { this.certificate = cert; this.loadingRef = false; },
          error: () => {
            this.loadingRef = false;
            this.toast.error(this.translate.instant('PREPARE.NO_CERTIFICATE'));
          }
        });
      },
      error: () => {
        this.loadingRef = false;
        this.toast.error(this.translate.instant('PREPARE.ORDER_NOT_FOUND'));
      }
    });
  }

  downloadRefXml(): void {
    if (!this.certificate) return;
    this.orderService.downloadWithholdingCertificateXml(this.certificate.orderId);
  }

  downloadRefPdf(): void {
    if (!this.certificate) return;
    this.orderService.downloadWithholdingCertificatePdf(this.certificate.orderId);
  }

  markRefDeposited(): void {
    if (!this.certificate || !this.refTejReference.trim()) return;
    this.savingDeposit = true;
    this.orderService.markCertificateDeposited(this.certificate.orderId, this.refTejReference.trim()).subscribe({
      next: (cert) => {
        this.certificate = cert;
        this.refTejReference = '';
        this.savingDeposit = false;
        this.toast.success(this.translate.instant('PAYMENT_DETAIL.CERT_DEPOSITED'));
      },
      error: (e: any) => {
        this.toast.error(e.error?.message || this.translate.instant('PAYMENTS.ERROR'));
        this.savingDeposit = false;
      }
    });
  }

  // ── Saisie libre ───────────────────────────────────────────────
  get manualComputation() {
    return computeManualWithholding(
      Number(this.form.grossAmount) || 0,
      this.form.nature,
      this.form.beneficiaryMatricule
    );
  }

  get manualDueDate(): string {
    if (!this.form.paymentDate) return '';
    return tejDueDate(this.form.paymentDate);
  }

  canBuildManualXml(): boolean {
    const c = this.manualComputation;
    return !!this.form.debtorName.trim() && !!this.form.beneficiaryName.trim()
      && (Number(this.form.grossAmount) || 0) > 0 && !!this.form.paymentDate
      && !!this.form.paymentReference.trim() && c.withholding > 0;
  }

  buildManualXml(): void {
    if (!this.canBuildManualXml()) return;
    const c = this.manualComputation;
    const gross = Number(this.form.grossAmount) || 0;
    this.manualXml = buildTejXml({
      number: this.form.number.trim() || `CRT-MAN-${Date.now()}`,
      debtorName: this.form.debtorName.trim(),
      debtorMatricule: this.form.debtorMatricule.trim(),
      debtorAddress: this.form.debtorAddress.trim(),
      beneficiaryName: this.form.beneficiaryName.trim(),
      beneficiaryMatricule: this.form.beneficiaryMatricule.trim(),
      beneficiaryAddress: this.form.beneficiaryAddress.trim(),
      nature: c.nature,
      grossAmount: gross,
      rate: c.rate,
      withholdingAmount: c.withholding,
      netAmount: c.net,
      currency: 'TND',
      paymentReference: this.form.paymentReference.trim(),
      paymentDate: this.form.paymentDate,
      billingPeriod: this.form.billingPeriod.trim()
        || this.form.paymentDate.slice(0, 7),
      dueDate: this.manualDueDate,
      depositorTejId: this.form.depositorTejId.trim() || null
    });
  }

  downloadManualXml(): void {
    if (!this.manualXml) return;
    saveBlob(new Blob([this.manualXml], { type: 'application/xml' }),
      `tej-retenue-${this.form.paymentReference.trim() || 'manuelle'}.xml`);
  }

  async onPdfSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files[0];
    if (!file) return;
    this.pdfFileName = file.name;
    this.pdfParsing = true;
    this.pdfHints = [];
    try {
      const text = await this.pdfText.extractText(file);
      const hints = extractPdfHints(text);
      const applied: string[] = [];
      if (hints.matricules.length > 0 && !this.form.beneficiaryMatricule) {
        this.form.beneficiaryMatricule = hints.matricules[0];
        applied.push(hints.matricules[0]);
      }
      if (hints.matricules.length > 1 && !this.form.debtorMatricule) {
        this.form.debtorMatricule = hints.matricules[1];
        applied.push(hints.matricules[1]);
      }
      if (hints.amounts.length > 0 && !this.form.grossAmount) {
        this.form.grossAmount = hints.amounts[0];
        applied.push(String(hints.amounts[0]));
      }
      this.pdfHints = applied;
      if (applied.length === 0) {
        this.toast.error(this.translate.instant('PREPARE.PDF_NO_HINT'));
      }
    } catch {
      this.toast.error(this.translate.instant('PREPARE.PDF_PARSE_ERROR'));
    } finally {
      this.pdfParsing = false;
    }
  }
}
