import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { Order } from '../../models/order.model';
import { paginateItems } from '../../models/page.model';
import { ToastService } from '../../services/toast.service';
import { LoginService } from '../../services/login.service';
import { ConfirmDialogService } from '../../components/confirm-dialog/confirm-dialog.service';

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
  loadingDetail = false;

  /** Pagination par section (endpoints livraisons non paginés côté serveur). */
  sectionPages: { [key: string]: number } = {};
  sectionSize = 10;

  constructor(
    private orderService: OrderService,
    private route: ActivatedRoute,
    private toast: ToastService,
    private loginService: LoginService,
    private confirmDialog: ConfirmDialogService
  ) {}

  /** Indique si l'utilisateur connecté est un profil boutique (accès réceptions). */
  isShopUser(): boolean {
    const roles = this.loginService.getCurrentUser()?.roles || [];
    return roles.includes('SHOP_ADMIN') || roles.includes('SHOP_AGENT');
  }

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
        this.sectionPages = {};
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
    return this.deliveries.filter(d => d.status === 'DELIVERY_REJECTED' || d.status === 'CANCELLED');
  }

  /** Livraisons en attente de réception boutique (acceptation sous 15 min sinon auto). */
  get pendingReception(): Order[] {
    return this.deliveries.filter(d => d.status === 'DELIVERED');
  }

  /** Accepte la réception d'une commande livrée (profil boutique). */
  acceptReception(order: Order): void {
    this.orderService.accept(order.id).subscribe({
      next: () => {
        this.toast.success('Réception acceptée');
        this.loadDeliveries();
      },
      error: (err: any) => {
        this.toast.error(err.error?.message || 'Erreur');
      }
    });
  }

  /** Rejette la réception via popup (profil boutique). */
  rejectReception(order: Order): void {
    this.confirmDialog.confirm({
      title: 'Rejeter la réception',
      message: `Rejeter la réception de ${order.reference} ?`,
      danger: true,
    }).subscribe(ok => {
      if (!ok) return;
      this.orderService.reject(order.id).subscribe({
        next: () => {
          this.toast.success('Réception rejetée');
          this.loadDeliveries();
        },
        error: (err: any) => {
          this.toast.error(err.error?.message || 'Erreur');
        }
      });
    });
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
        this.toast.success('Livraison rejetée, commande annulée');
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
        this.toast.success('Livraison confirmée, paiement créé');
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
      DELIVERY_REJECTED: 'Rejetée',
      CANCELLED: 'Annulée'
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
      DELIVERY_REJECTED: 'rejected',
      CANCELLED: 'cancelled'
    };
    return map[s] || '';
  }

  /** Ouvre le panneau de détail d'une commande (appel ciblé par id). */
  openDetail(order: Order): void {
    this.selectedDetailOrder = null;
    this.loadingDetail = true;
    this.showDetail = true;
    this.orderService.getById(order.id).subscribe({
      next: (data: Order) => { this.selectedDetailOrder = data; this.loadingDetail = false; },
      error: () => { this.selectedDetailOrder = order; this.loadingDetail = false; }
    });
  }

  /** Ferme le panneau de détail d'une commande. */
  closeDetail(): void {
    this.showDetail = false;
    this.selectedDetailOrder = null;
    this.loadingDetail = false;
  }

  /** Page d'une section de livraisons (pagination côté client). */
  paged(list: Order[], key: string): Order[] {
    return paginateItems(list, this.sectionPages[key] || 0, this.sectionSize);
  }

  onSectionPage(key: string, page: number): void {
    this.sectionPages[key] = page;
  }

  onSectionSize(size: number): void {
    this.sectionSize = size;
    this.sectionPages = {};
  }
}
