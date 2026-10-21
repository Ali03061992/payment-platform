import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { WithholdingCertificate } from '../../models/order.model';

@Component({
  selector: 'app-document-certificate',
  templateUrl: './certificate-document.component.html',
  styleUrls: ['./certificate-document.component.css'],
  standalone: false
})
export class CertificateDocumentComponent implements OnInit {
  certificate: WithholdingCertificate | null = null;
  loading = true;
  error = '';

  constructor(private route: ActivatedRoute, private orders: OrderService) {}

  ngOnInit(): void {
    const number = this.route.snapshot.paramMap.get('number') || '';
    this.orders.getWithholdingCertificateByNumber(number).subscribe({
      next: (certificate) => {
        this.certificate = certificate;
        this.loading = false;
      },
      error: () => {
        this.error = 'DOCUMENTS.NOT_FOUND';
        this.loading = false;
      }
    });
  }

  downloadPdf(): void {
    if (this.certificate) this.orders.downloadWithholdingCertificatePdf(this.certificate.orderId);
  }

  downloadXml(): void {
    if (this.certificate) this.orders.downloadWithholdingCertificateXml(this.certificate.orderId);
  }
}
