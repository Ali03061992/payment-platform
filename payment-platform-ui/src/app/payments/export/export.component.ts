import { Component } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';

@Component({
    selector: 'app-export',
    templateUrl: './export.component.html',
    styleUrls: ['./export.component.css'],
    standalone: false
})
export class ExportComponent {
  from = '';
  to = '';
  loading = false;
  error = '';

  constructor(private http: HttpClient) {
    const today = new Date();
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
    this.to = this.formatDate(today);
    this.from = this.formatDate(firstDay);
  }

  private formatDate(d: Date): string {
    return d.toISOString().split('T')[0];
  }

  exportCsv(): void {
    if (!this.from || !this.to) {
      this.error = 'Veuillez sélectionner les dates.';
      return;
    }
    this.loading = true;
    this.error = '';

    const params = new HttpParams()
      .set('from', this.from)
      .set('to', this.to);

    this.http.get('/api/payments/export', {
      params,
      responseType: 'blob'
    }).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `payments_${this.from}_${this.to}.csv`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        a.remove();
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors de l\'export.';
        this.loading = false;
      }
    });
  }
}
