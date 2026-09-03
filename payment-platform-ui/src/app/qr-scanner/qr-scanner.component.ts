import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { Router } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ToastService } from '../services/toast.service';

@Component({
  selector: 'app-qr-scanner',
  templateUrl: './qr-scanner.component.html',
  styleUrls: ['./qr-scanner.component.css']
})
export class QrScannerComponent implements OnInit, OnDestroy {
  @ViewChild('video') videoRef!: ElementRef<HTMLVideoElement>;

  scanning = false;
  manualUrl = '';
  useManual = false;
  cameraActive = false;
  private stream: MediaStream | null = null;
  private scanInterval: any;

  constructor(private router: Router, private sanitizer: DomSanitizer, private toast: ToastService) {}

  ngOnInit(): void {
    if (!this.isCameraSupported()) {
      this.useManual = true;
    }
  }

  ngOnDestroy(): void {
    this.stopCamera();
  }

  isCameraSupported(): boolean {
    return !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia);
  }

  async startCamera(): Promise<void> {
    this.useManual = false;
    try {
      this.stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment' }
      });
      this.cameraActive = true;
      this.scanning = true;

      setTimeout(() => {
        if (this.videoRef?.nativeElement) {
          this.videoRef.nativeElement.srcObject = this.stream;
          this.videoRef.nativeElement.play();
          this.startBarcodeDetection();
        }
      }, 100);
    } catch (err: any) {
      this.toast.error('Caméra non disponible. Utilisez la saisie manuelle.');
      this.useManual = true;
    }
  }

  startBarcodeDetection(): void {
    const BD = (window as any).BarcodeDetector;
    if (BD) {
      const barcodeDetector = new BD({ formats: ['qr_code'] });
      this.scanInterval = setInterval(async () => {
        if (this.videoRef?.nativeElement && this.videoRef.nativeElement.readyState >= 2) {
          try {
            const barcodes = await barcodeDetector.detect(this.videoRef.nativeElement);
            if (barcodes.length > 0) {
              this.handleResult(barcodes[0].rawValue);
            }
          } catch {}
        }
      }, 500);
    } else {
      this.toast.error('Détection QR auto non supportée. Utilisez la saisie manuelle.');
      this.stopCamera();
      this.useManual = true;
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
    if (this.scanInterval) {
      clearInterval(this.scanInterval);
      this.scanInterval = null;
    }
    if (this.stream) {
      this.stream.getTracks().forEach(t => t.stop());
      this.stream = null;
    }
    this.cameraActive = false;
    this.scanning = false;
  }

  goBack(): void {
    this.stopCamera();
    this.router.navigate(['/dashboard']);
  }
}
