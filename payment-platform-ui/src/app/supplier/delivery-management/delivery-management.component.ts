import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { Order } from '../../models/order.model';
import { ToastService } from '../../services/toast.service';

/**
 * Écran livreur/admin des livraisons (acceptation, confirmation de date, livraison, rejet).
 * Charge les livraisons visibles et pilote les modales d'action.
 */
@Component({
    selector: 'app-delivery-management',
    templateUrl: './delivery-management.component.html',
    styleUrls: ['./delivery-management.component.css'],
    standalone: false
})
export class DeliveryManagementComponent implements OnInit {
  deliveries: Order[] = [];
  loading = true;

  showDeliverModal = false;
  selectedOrder: Order | null = null;
  receivedBy = '';
  delivering = false;
  shopAgents: {id: string, name: string, roles?: string}[] = [];
  loadingShopAgents = false;

  /** Indique si une entrée d'agent porte le rôle boutique admin. */
  isShopAdmin(entry: {roles?: string}): boolean {
    return (entry.roles || '').split(',').includes('SHOP_ADMIN');
  }

  showConfirmDateModal = false;
  confirmDateOrder: Order | null = null;
  confirmedDate = '';
  confirming = false;

  showRejectModal = false;
  rejectOrder: Order | null = null;
  rejectReason = '';
  rejecting = false;

  highlightedOrderId: string | null = null;

  showDetail = false;
  selectedDetailOrder: Order | null = null;

  constructor(
    private orderService: OrderService,
    private route: ActivatedRoute,
    private toast: ToastService
  ) {}

  /** Initialise la surbrillance éventuelle puis charge les livraisons. */
  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.highlightedOrderId = params['orderId'] || null;
    });
    this.loadDeliveries();
  }

  /** Charge les livraisons de l'acteur et surligne la commande ciblée si demandée. */
  loadDeliveries(): void {
    this.loading = true;
    this.orderService.myDeliveries().subscribe({
      next: (data: Order[]) => {
        this.deliveries = data;
        this.loading = false;
        if (this.highlightedOrderId) {
          const id = this.highlightedOrderId;
          this.highlightedOrderId = null;
          setTimeout(() => {
            const el = document.getElementById('delivery-' + id);
            if (el) {
              el.scrollIntoView({ behavior: 'smooth', block: 'center' });
              el.classList.add('highlight-pulse');
              setTimeout(() => el.classList.remove('highlight-pulse'), 2000);
            }
          }, 300);
        }
      },
      error: (err: any) => {
        console.error('Deliveries error:', err);
        this.toast.error(err.error?.message || 'Erreur de chargement');
        this.loading = false;
      }
    });
  }

  get pendingAcceptance(): Order[] {
    return this.deliveries.filter(d => d.status === 'READY_FOR_DELIVERY');
  }

  get activeDeliveries(): Order[] {
    return this.deliveries.filter(d => d.status === 'DELIVERY_ACCEPTED' || d.status === 'IN_DELIVERY');
  }

  get completedDeliveries(): Order[] {
    return this.deliveries.filter(d => d.status === 'DELIVERED' || d.status === 'ACCEPTED');
  }

  get rejectedDeliveries(): Order[] {
    return this.deliveries.filter(d => d.status === 'DELIVERY_REJECTED');
  }

  /** Accepte la livraison d'une commande prête. */
  acceptDelivery(order: Order): void {
    this.orderService.acceptDelivery(order.id, true).subscribe({
      next: () => {
        this.toast.success('Livraison acceptée');
        this.loadDeliveries();
      },
      error: (err: any) => {
        this.toast.error(err.error?.message || 'Erreur');
      }
    });
  }

  /** Ouvre la modale de rejet pour une commande. */
  openRejectModal(order: Order): void {
    this.rejectOrder = order;
    this.rejectReason = '';
    this.showRejectModal = true;
  }

  /** Ferme la modale de rejet et réinitialise sa saisie. */
  closeRejectModal(): void {
    this.showRejectModal = false;
    this.rejectOrder = null;
    this.rejectReason = '';
  }

  /** Soumet le rejet de la livraison sélectionnée avec motif éventuel. */
  submitReject(): void {
    if (!this.rejectOrder) return;
    this.rejecting = true;
    this.orderService.acceptDelivery(this.rejectOrder.id, false, this.rejectReason).subscribe({
      next: () => {
        this.toast.success('Livraison rejetée');
        this.closeRejectModal();
        this.rejecting = false;
        this.loadDeliveries();
      },
      error: (err: any) => {
        this.toast.error(err.error?.message || 'Erreur');
        this.rejecting = false;
      }
    });
  }

  /** Ouvre la modale de confirmation de date pour une commande. */
  openConfirmDate(order: Order): void {
    this.confirmDateOrder = order;
    this.confirmedDate = order.plannedDeliveryDate || '';
    this.showConfirmDateModal = true;
  }

  /** Ferme la modale de confirmation de date. */
  closeConfirmDate(): void {
    this.showConfirmDateModal = false;
    this.confirmDateOrder = null;
  }

  /** Soumet la date de livraison confirmée pour la commande sélectionnée. */
  submitConfirmDate(): void {
    if (!this.confirmDateOrder || !this.confirmedDate) return;
    this.confirming = true;
    this.orderService.confirmDelivery(this.confirmDateOrder.id, this.confirmedDate).subscribe({
      next: () => {
        this.toast.success('Date de livraison confirmée');
        this.closeConfirmDate();
        this.confirming = false;
        this.loadDeliveries();
      },
      error: (err: any) => {
        this.toast.error(err.error?.message || 'Erreur');
        this.confirming = false;
      }
    });
  }

  /** Ouvre la modale de livraison et charge les agents de la boutique destinataire. */
  openDeliver(order: Order): void {
    this.selectedOrder = order;
    this.receivedBy = '';
    this.shopAgents = [];
    this.loadingShopAgents = true;
    this.showDeliverModal = true;
    if (order.shopId) {
      this.orderService.getShopAgents(order.shopId).subscribe({
        next: (agents) => { this.shopAgents = agents; this.loadingShopAgents = false; },
        error: () => { this.shopAgents = []; this.loadingShopAgents = false; }
      });
    } else {
      this.loadingShopAgents = false;
    }
  }

  /** Ferme la modale de livraison. */
  closeDeliver(): void {
    this.showDeliverModal = false;
    this.selectedOrder = null;
  }

  /** Confirme la livraison auprès du destinataire sélectionné. */
  confirmDeliver(): void {
    if (!this.selectedOrder || !this.receivedBy) return;
    this.delivering = true;
    this.orderService.deliver(this.selectedOrder.id, this.receivedBy).subscribe({
      next: () => {
        this.toast.success('Livraison confirmée avec succès');
        this.closeDeliver();
        this.delivering = false;
        this.loadDeliveries();
      },
      error: (err: any) => {
        this.toast.error(err.error?.message || 'Erreur');
        this.delivering = false;
      }
    });
  }

  /** Traduit un statut de livraison en libellé français d'affichage. */
  statusLabel(s: string): string {
    const map: Record<string, string> = {
      READY_FOR_DELIVERY: 'En attente d\'acceptation',
      DELIVERY_ACCEPTED: 'Acceptée - En attente de livraison',
      IN_DELIVERY: 'En livraison',
      DELIVERED: 'Livré',
      ACCEPTED: 'Accepté par la boutique',
      DELIVERY_REJECTED: 'Rejetée'
    };
    return map[s] || s;
  }

  /** Traduit un statut de livraison en classe CSS de badge. */
  statusClass(s: string): string {
    const map: Record<string, string> = {
      READY_FOR_DELIVERY: 'pending',
      DELIVERY_ACCEPTED: 'confirmed',
      IN_DELIVERY: 'in-delivery',
      DELIVERED: 'delivered',
      ACCEPTED: 'accepted',
      DELIVERY_REJECTED: 'rejected'
    };
    return map[s] || '';
  }

  /** Ouvre le panneau de détail d'une commande. */
  openDetail(order: Order): void {
    this.selectedDetailOrder = order;
    this.showDetail = true;
  }

  /** Ferme le panneau de détail d'une commande. */
  closeDetail(): void {
    this.showDetail = false;
    this.selectedDetailOrder = null;
  }
}
