import { Injectable } from '@angular/core';

/**
 * Extrait le texte d'une facture PDF (pdf.js, worker packagé dans assets/)
 * pour pré-remplir la saisie libre du dépôt TEJ. Heuristiques seulement :
 * toute valeur proposée doit être vérifiée par l'utilisateur.
 */
@Injectable({ providedIn: 'root' })
export class PdfInvoiceTextService {
  private workerReady = false;

  async extractText(file: File): Promise<string> {
    const pdfjs: any = await import('pdfjs-dist');
    if (!this.workerReady) {
      pdfjs.GlobalWorkerOptions.workerSrc = 'assets/pdf.worker.min.mjs';
      this.workerReady = true;
    }
    const buffer = await file.arrayBuffer();
    const doc = await pdfjs.getDocument({ data: new Uint8Array(buffer) }).promise;
    const parts: string[] = [];
    const pages = Math.min(doc.numPages, 10);
    for (let i = 1; i <= pages; i++) {
      const page = await doc.getPage(i);
      const content = await page.getTextContent();
      parts.push((content.items as any[]).map((it) => it.str || '').join(' '));
    }
    await doc.destroy();
    return parts.join('\n');
  }
}
