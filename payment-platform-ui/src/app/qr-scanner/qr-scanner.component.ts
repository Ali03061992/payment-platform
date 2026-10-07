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
    if (data) {
      const paymentId = this.extractPaymentId(data);
      if (paymentId) {
        this.router.navigate(['/dashboard/payments', paymentId]);
      } else {
        this.toast.error(this.translate.instant('QR.ERR_NOT_RECOGNIZED'));
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
