import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { QuoteService } from '../../services/quote.service';
import { QuoteRequest, QuoteLine } from '../../models/order.model';

@Component({
  selector: 'app-document-quote',
  templateUrl: './quote-document.component.html',
  styleUrls: ['./quote-document.component.css'],
  standalone: false
})
export class QuoteDocumentComponent implements OnInit {
  request: QuoteRequest | null = null;
  lines: QuoteLine[] = [];
  loading = true;
  error = '';

  constructor(private route: ActivatedRoute, private quotes: QuoteService) {}

  ngOnInit(): void {
    const reference = this.route.snapshot.paramMap.get('reference') || '';
    this.quotes.getRequestByReference(reference).subscribe({
      next: (request) => {
        this.request = request;
        this.lines = this.quotes.parseLines(request.linesJson);
        this.loading = false;
      },
      error: () => {
        this.error = 'DOCUMENTS.NOT_FOUND';
        this.loading = false;
      }
    });
  }

  download(version: number): void {
    if (this.request) this.quotes.downloadQuotePdf(this.request.id, version, this.request.reference);
  }
}
