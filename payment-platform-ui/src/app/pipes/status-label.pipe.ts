import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'statusLabel',
    standalone: false
})
export class StatusLabelPipe implements PipeTransform {
  private labels: Record<string, string> = {
    'PENDING': 'En attente',
    'DRAFT': 'Brouillon',
    'CONFIRMED': 'Confirmé',
    'PREPARING': 'En préparation',
    'READY_FOR_DELIVERY': 'Prêt pour livraison',
    'IN_DELIVERY': 'En livraison',
    'DELIVERED': 'Livré',
    'ACCEPTED': 'Accepté',
    'CANCELLED': 'Annulé',
    'REJECTED': 'Rejeté',
    'DELIVERY_REJECTED': 'Livraison rejetée',
  };

  transform(status: string): string {
    return this.labels[status] || status;
  }
}
