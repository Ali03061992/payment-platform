import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { ToastService } from '../services/toast.service';
import { BrowserMultiFormatReader } from '@zxing/library';

@Component({
    selector: 'app-qr-scanner',
    templateUrl: './qr-scanner.component.html',
    styleUrls: ['./qr-scanner.component.css'],
    standalone: false
})
export class QrScannerComponent implements OnInit, OnDestroy {
  @ViewChild('video') videoRef!: ElementRef<HTMLVideoElement>;

  scanning = false;
  manualUrl = '';
  useManual = true;
  cameraActive = false;
  cameraError = '';
  isSecureContext = false;
  private codeReader: BrowserMultiFormatReader | null = null;

  constructor(private router: Router, private toast: ToastService, private translate: TranslateService) {}

  async ngOnInit(): Promise<void> {
    this.isSecureContext = window.isSecureContext;
    // Caméra forcée à l'ouverture : la permission navigateur est demandée
    // immédiatement (sur iOS Safari la vérification user-gesture échoue proprement
    // et le bouton Réessayer/Activer relance la demande dans un geste utilisateur).
    if (this.isSecureContext && this.isCameraSupported()) {
      await this.startCamera();
    }
  }

  /** Relance la demande de permission caméra (bouton Réessayer). */
  async retryCamera(): Promise<void> {
    await this.startCamera();
  }

  ngOnDestroy(): void {
    this.stopCamera();
  }

  isCameraSupported(): boolean {
    return !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia);
  }

  async startCamera(): Promise<void> {
    this.cameraError = '';
    this.useManual = false;

    if (!this.isSecureContext) {
      this.cameraError = this.translate.instant('QR.ERR_HTTPS');
      this.useManual = true;
      return;
    }

    if (!this.isCameraSupported()) {
      this.cameraError = this.translate.instant('QR.ERR_UNSUPPORTED');
      this.useManual = true;
      return;
    }

    try {
      this.codeReader = new BrowserMultiFormatReader();

      const devices = await this.codeReader.getVideoInputDevices();
      if (devices.length === 0) {
        this.cameraError = this.translate.instant('QR.ERR_NO_CAMERA');
        this.useManual = true;
        return;
      }

      this.cameraActive = true;
      this.scanning = true;

      const rearCamera = devices.find(d =>
        d.label.toLowerCase().includes('back') ||
        d.label.toLowerCase().includes('rear') ||
        d.label.toLowerCase().includes('environment')
      ) || devices[devices.length - 1];

      await this.codeReader.decodeFromVideoDevice(
        rearCamera.deviceId,
        this.videoRef.nativeElement,
        (result, err) => {
          if (result) {
            this.handleResult(result.getText());
          }
        }
      );
    } catch (e: any) {
      const msg = e?.message || String(e);
      if (msg.includes('Permission') || msg.includes('denied') || msg.includes('NotAllowedError')) {
        this.cameraError = this.translate.instant('QR.ERR_PERMISSION');
      } else {
        this.cameraError = this.translate.instant('QR.ERR_GENERIC', { msg });
      }
      this.useManual = true;
      this.cameraActive = false;
    }
  }

  handleResult(data: string): void {
    this.stopCamera();
    if (!data || !data.trim()) return;
    const route = this.resolveRoute(data);
    if (route) {
      void this.router.navigateByUrl(route);
    } else {
      this.toast.error(this.translate.instant('QR.ERR_NOT_RECOGNIZED'));
    }
  }

  /**
   * Résout un QR/lien vers une route interne. Accepte les liens NEXTERA
   * (paiement, facture, commande, litige…) et les références de paiement.
   * Une URL externe est réduite à son chemin : seules les routes de l'app
   * sont ouvertes (pas d'open-redirect).
   */
  resolveRoute(data: string): string | null {
    const raw = (data || '').trim();
    if (!raw) return null;

    const paymentId = this.extractPaymentId(raw);
    if (paymentId !== null) return `/dashboard/payments/${paymentId}`;
    if (/^PAY-[A-Za-z0-9-]+$/i.test(raw)) {
      return `/dashboard/payments/${encodeURIComponent(raw)}`;
    }

    let path = raw;
    if (!path.startsWith('/')) {
      try {
        const parsed = new URL(path);
        if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') return null;
        path = `${parsed.pathname}${parsed.search}${parsed.hash}`;
      } catch {
        return null;
      }
    }

    if (path.startsWith('/payments/')) {
      path = `/dashboard${path}`;
    }
    const isDashboard = path === '/dashboard' || path.startsWith('/dashboard/')
      || path.startsWith('/dashboard?') || path.startsWith('/dashboard#');
    return isDashboard ? path : null;
  }

  extractPaymentId(data: string): number | null {
    const urlPatterns = [
      /\/dashboard\/payments\/(\d+)/,
      /\/payments\/(\d+)/,
      /payment[_-]?id[=:](\d+)/i,
      /facture[=:](\d+)/i,
    ];
    for (const pattern of urlPatterns) {
      const match = data.match(pattern);
      if (match) return parseInt(match[1], 10);
    }
    const plainNumber = data.trim();
    if (/^\d+$/.test(plainNumber)) {
      return parseInt(plainNumber, 10);
    }
    return null;
  }

  submitManual(): void {
    const raw = this.manualUrl.trim();
    if (!raw) return;
    const route = this.resolveRoute(raw);
    if (route) {
      this.stopCamera();
      void this.router.navigateByUrl(route);
    } else {
      this.toast.error(this.translate.instant('QR.ERR_UNRECOGNIZED'));
    }
  }

  stopCamera(): void {
    if (this.codeReader) {
      this.codeReader.reset();
      this.codeReader = null;
    }
    this.cameraActive = false;
    this.scanning = false;
  }

  goBack(): void {
    this.stopCamera();
    this.router.navigate(['/dashboard']);
  }
}
