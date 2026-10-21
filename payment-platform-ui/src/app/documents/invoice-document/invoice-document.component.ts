import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { InvoiceJournalEntry, Order } from '../../models/order.model';

@Component({
  selector: 'app-document-invoice',
  templateUrl: './invoice-document.component.html',
  styleUrls: ['./invoice-document.component.css'],
  standalone: false
})
export class InvoiceDocumentComponent implements OnInit {
  invoice: InvoiceJournalEntry | null = null;
  order: Order | null = null;
  loading = true;
  error = '';

  constructor(private route: ActivatedRoute, private orders: OrderService) {}

  ngOnInit(): void {
    const number = this.route.snapshot.paramMap.get('invoiceNumber') || '';
    this.orders.getInvoiceByNumber(number).subscribe({
      next: (invoice) => {
        this.invoice = invoice;
        this.orders.getById(invoice.orderId).subscribe({
          next: (order) => {
            this.order = order;
            this.loading = false;
          },
          error: () => {
            this.loading = false;
          }
        });
      },
      error: () => {
        this.error = 'DOCUMENTS.NOT_FOUND';
        this.loading = false;
      }
    });
  }

  downloadInvoice(): void {
    if (this.invoice) this.orders.downloadInvoice(this.invoice.orderId);
  }
}
