import { Optional, Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

export const STATUS_LABELS: Record<string, string> = {
  'PENDING': 'En attente',
  'DRAFT': 'Brouillon',
  'CONFIRMED': 'Confirmé',
  'PREPARING': 'En préparation',
  'READY_FOR_DELIVERY': 'Prêt pour livraison',
  'DELIVERY_ACCEPTED': 'Livraison acceptée',
  'IN_DELIVERY': 'En livraison',
  'DELIVERED': 'Livré',
  'ACCEPTED': 'Accepté',
  'CANCELLED': 'Annulé',
  'REJECTED': 'Rejeté',
  'DELIVERY_REJECTED': 'Livraison rejetée',
  'ACTIVE': 'Actif',
  'INACTIVE': 'Inactif',
  'OUT_OF_STOCK': 'Rupture',
  'OK': 'En stock',
  'LOW': 'Stock bas',
};

export function statusLabelFr(status: string): string {
  return STATUS_LABELS[status] || status;
}

@Pipe({
    name: 'statusLabel',
    standalone: false,
    pure: false
})
export class StatusLabelPipe implements PipeTransform {
  constructor(@Optional() private translate?: TranslateService) {}

  transform(status: string): string {
    if (this.translate) {
      const key = 'STATUS.' + status;
      const v = this.translate.instant(key);
      if (v && v !== key) return v;
    }
    return statusLabelFr(status);
  }
}
