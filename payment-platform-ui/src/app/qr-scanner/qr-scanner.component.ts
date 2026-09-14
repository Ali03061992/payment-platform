import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { Router } from '@angular/router';
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

  constructor(private router: Router, private toast: ToastService) {}

  ngOnInit(): void {
    this.isSecureContext = window.isSecureContext;
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
      this.cameraError = 'La caméra nécessite HTTPS. Accédez via https:// ou utilisez la saisie manuelle.';
      this.useManual = true;
      return;
    }

    if (!this.isCameraSupported()) {
      this.cameraError = 'Caméra non supportée par ce navigateur.';
      this.useManual = true;
      return;
    }

    try {
      this.codeReader = new BrowserMultiFormatReader();

      const devices = await this.codeReader.getVideoInputDevices();
      if (devices.length === 0) {
        this.cameraError = 'Aucune caméra détectée.';
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
        this.cameraError = 'Permission caméra refusée. Autorisez l\'accès dans les paramètres du navigateur.';
      } else {
        this.cameraError = 'Erreur caméra: ' + msg;
      }
      this.useManual = true;
      this.cameraActive = false;
    }
  }

  handleResult(data: string): void {
    this.stopCamera();
    if (data) {
      const paymentId = this.extractPaymentId(data);
      if (paymentId) {
        this.router.navigate(['/dashboard/payments', paymentId]);
      } else {
        this.toast.error('QR Code non reconnu comme facture valide.');
      }
    }
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
    if (!this.manualUrl.trim()) return;
    const paymentId = this.extractPaymentId(this.manualUrl.trim());
    if (paymentId) {
      this.router.navigate(['/dashboard/payments', paymentId]);
    } else {
      this.toast.error('Lien ou ID non reconnu. Essayez un numéro de facture.');
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
