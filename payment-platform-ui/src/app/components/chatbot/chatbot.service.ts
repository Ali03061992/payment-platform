import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { DisputeService } from '../../services/dispute.service';
import { LoginService } from '../../services/login.service';
import { OrderService } from '../../services/order.service';
import { PaymentService } from '../../services/payment.service';

export interface ChatLink { label: string; route: string; }
export interface ChatMessage {
  from: 'bot' | 'user';
  text: string;
  links?: ChatLink[];
  options?: string[];
}

type BotState =
  | 'idle'
  | 'awaitOrderRef'
  | 'awaitPendingChoice'
  | 'awaitComplaintOrder'
  | 'awaitComplaintReasonText'
  | 'awaitComplaintConfirm';

/**
 * Petit assistant conversationnel (100% local, sans IA externe) : suivi de
 * commande, dépôt guidé de réclamation (litige), aide paiements/livraisons.
 */
@Injectable({ providedIn: 'root' })
export class ChatbotService {
  private messagesSubject = new BehaviorSubject<ChatMessage[]>([]);
  messages$ = this.messagesSubject.asObservable();

  private state: BotState = 'idle';
  private pendingOrderId: string | null = null;
  private pendingOrderRef: string | null = null;
  private pendingReason: string | null = null;
  private started = false;

  constructor(
    private router: Router,
    private orders: OrderService,
    private disputes: DisputeService,
    private payments: PaymentService,
    private loginService: LoginService
  ) {}

  start(): void {
    if (this.started) return;
    this.started = true;
    this.say(
      `Bonjour ${this.firstName()} ! Je suis l'assistant de la plateforme. Je peux suivre une commande, vous aider à déposer une réclamation ou vous guider vers la bonne page.`,
      undefined,
      ['Suivre une commande', 'Déposer une réclamation', 'Aide paiements', 'Aide livraisons']
    );
  }

  reset(): void {
    this.state = 'idle';
    this.pendingOrderId = null;
    this.pendingOrderRef = null;
    this.pendingReason = null;
    this.say('Que puis-je faire pour vous ?',
      undefined, ['Suivre une commande', 'Déposer une réclamation', 'Aide paiements', 'Aide livraisons']);
  }

  send(text: string): void {
    const clean = text.trim();
    if (!clean) return;
    this.push({ from: 'user', text: clean });
    const norm = clean.toLowerCase()
      .normalize('NFD').replace(/[\u0300-\u036f]/g, '');

    switch (this.state) {
      case 'awaitOrderRef': return this.handleOrderRef(clean);
      case 'awaitPendingChoice': return this.handlePendingChoice(norm);
      case 'awaitComplaintOrder': return this.handleComplaintOrder(clean);
      case 'awaitComplaintReasonText': return this.handleComplaintReasonText(clean);
      case 'awaitComplaintConfirm': return this.handleComplaintConfirm(norm);
      default: return this.handleIntent(norm, clean);
    }
  }

  choose(option: string): void {
    this.send(option);
  }

  // ── Intentions ──────────────────────────────────────────────

  private handleIntent(norm: string, raw: string): void {
    if (/(bonjour|salut|hello|coucou|bonsoir)/.test(norm)) {
      this.say(`Bonjour ${this.firstName()} !`, undefined,
        ['Suivre une commande', 'Déposer une réclamation', 'Aide paiements', 'Aide livraisons']);
      return;
    }
    if (/(commande|suivi|suivre|statut.*commande|ou en est)/.test(norm)) {
      this.state = 'awaitOrderRef';
      this.say('Quelle est la référence de la commande ? (ex. ORD-AB12CD34). Vous pouvez aussi taper « récentes » pour voir vos dernières commandes.');
      return;
    }
    if (/(reclamation|litige|probleme|plainte|sav|retour)/.test(norm)) {
      this.startComplaint();
      return;
    }
    if (/(paiement|payer|facture|balance|solde)/.test(norm)) {
      this.state = 'awaitPendingChoice';
      this.say(
        'Côté paiements : « En attente » = à traiter, « Confirmé » = validé, « Rejeté/Annulé » = clôturé sans suite. Voulez-vous voir vos paiements en attente ?',
        this.isShop()
          ? [{ label: 'Voir ma balance', route: '/dashboard/shop/balance' }]
          : [{ label: 'Voir les paiements', route: '/dashboard/payments' }],
        ['Oui, montrer', 'Non merci']
      );
      return;
    }
    if (/(livraison|livreur|colis|recu|reception)/.test(norm)) {
      this.say(
        'Une livraison passe par : assignation → acceptation → confirmation → réception. La réception confirmée crée automatiquement le paiement.',
        this.isShop()
          ? [{ label: 'Voir mes livraisons', route: '/dashboard/shop/deliveries' }]
          : [{ label: 'Voir les livraisons', route: '/dashboard/supplier/deliveries' }],
        ['Suivre une commande', 'Déposer une réclamation']
      );
      return;
    }
    if (/(merci)/.test(norm)) {
      this.say('Avec plaisir ! Autre chose ?',
        undefined, ['Suivre une commande', 'Déposer une réclamation']);
      return;
    }
    if (/(humain|conseiller|contact|telephone|email|aide)/.test(norm)) {
      this.say("Pour un cas particulier, décrivez-le dans une réclamation : l'équipe concernée vous répondra dans le fil du litige.",
        undefined, ['Déposer une réclamation']);
      return;
    }
    this.say(`Je n'ai pas bien compris « ${raw} ». Voici ce que je sais faire :`,
      undefined, ['Suivre une commande', 'Déposer une réclamation', 'Aide paiements', 'Aide livraisons']);
  }

  // ── Suivi commande ──────────────────────────────────────────

  private handleOrderRef(ref: string): void {
    if (/recentes?/.test(ref.toLowerCase())) {
      this.orders.getRecent(5).subscribe({
        next: (list) => {
          if (!list.length) {
            this.state = 'idle';
            this.say("Aucune commande récente trouvée. Donnez-moi une référence (ex. ORD-AB12CD34).");
            return;
          }
          this.state = 'awaitOrderRef';
          this.say('Voici vos dernières commandes : choisissez-en une (tapez sa référence).',
            list.map((o: any) => ({ label: `${o.reference} — ${this.orderStatusLabel(o.status)}`, route: '' })),
            list.map((o: any) => o.reference));
        },
        error: () => {
          this.state = 'idle';
          this.say("Impossible de charger les commandes. Réessayez plus tard.");
        }
      });
      return;
    }
    this.lookupOrder(ref);
  }

  private lookupOrder(ref: string): void {
    this.orders.getByReference(ref).subscribe({
      next: (o: any) => {
        this.state = 'idle';
        const detailRoute = this.isShop()
          ? `/dashboard/shop/orders/${o.id}`
          : `/dashboard/supplier/orders`;
        this.say(`Commande ${o.reference} : ${this.orderStatusLabel(o.status)} — total ${Number(o.total).toFixed(2)} ${o.currency || 'TND'}.`,
          [{ label: 'Voir le détail', route: detailRoute }],
          ['Suivre une commande', 'Déposer une réclamation']);
      },
      error: () => {
        this.say(`Commande « ${ref} » introuvable. Vérifiez la référence ou tapez « récentes ».`);
      }
    });
  }

  // ── Paiements en attente (lookup réel) ────────────────────────

  private handlePendingChoice(norm: string): void {
    if (!/^(oui|montrer|ok|oui,)/.test(norm)) {
      this.reset();
      return;
    }
    this.payments.list(0, 50).subscribe({
      next: (res: any) => {
        const arr: any[] = Array.isArray(res) ? res : res?.items || [];
        const pending = arr.filter((p) => p.status === 'PENDING');
        this.state = 'idle';
        if (!pending.length) {
          this.say('Bonne nouvelle : aucun paiement en attente sur les 50 derniers.',
            [{ label: 'Voir les paiements', route: '/dashboard/payments' }],
            ['Suivre une commande', 'Déposer une réclamation']);
          return;
        }
        const total = pending.reduce((s, p) => s + Number(p.amount || 0), 0);
        const refs = pending.slice(0, 3).map((p) => p.reference).join(', ');
        this.say(`${pending.length} paiement(s) en attente — total ${total.toFixed(2)} TND${refs ? ` (ex. ${refs})` : ''}.`,
          [{ label: 'Voir les paiements', route: '/dashboard/payments' }],
          ['Suivre une commande', 'Déposer une réclamation']);
      },
      error: () => {
        this.state = 'idle';
        this.say('Impossible de charger les paiements pour le moment.',
          [{ label: 'Voir les paiements', route: '/dashboard/payments' }],
          ['Suivre une commande', 'Déposer une réclamation']);
      }
    });
  }

  // ── Réclamation guidée ──────────────────────────────────────

  private startComplaint(): void {
    this.state = 'awaitComplaintOrder';
    this.say('Décrivez le problème en précisant la référence (ex. « colis cassé ORD-A1B2C3 ») : je crée la réclamation directement. Sinon, donnez juste la référence ou tapez « récentes ».');
  }

  /** Création directe : référence + motif dans le même message, zéro question. */
  private tryDirectComplaint(text: string): boolean {
    const m = text.toUpperCase().match(/ORD-[A-Z0-9]{4,}/);
    if (!m) return false;
    const reason = text.replace(/ORD-[A-Z0-9]{4,}/i, '').replace(/["'«»]/g, '').trim();
    if (reason.replace(/[^a-zA-Z0-9]/g, '').length < 3) return false;
    this.orders.getByReference(m[0]).subscribe({
      next: (o: any) => {
        this.createDisputeNow(o.id, o.reference, reason.length > 300 ? reason.slice(0, 300) : reason);
      },
      error: () => {
        this.say(`Commande « ${m[0]} » introuvable. Vérifiez la référence ou tapez « récentes ».`);
      }
    });
    return true;
  }

  private createDisputeNow(orderId: string, ref: string, reason: string): void {
    this.disputes.create({ orderId, reason }).subscribe({
      next: (d: any) => {
        this.state = 'idle';
        this.pendingOrderId = this.pendingReason = this.pendingOrderRef = null;
        const detailRoute = this.isShop()
          ? `/dashboard/shop/disputes/${d.id}`
          : `/dashboard/shop/orders`;
        this.say(`Réclamation enregistrée pour ${ref} : « ${reason} ». L'équipe concernée vous répondra dans le fil du litige.`,
          [{ label: 'Voir la réclamation', route: detailRoute }],
          ['Suivre une commande', 'Déposer une réclamation']);
      },
      error: (e: any) => {
        this.state = 'idle';
        this.say(`Échec de l'enregistrement (${e.error?.message || 'erreur'}). Réessayez ou passez par la page commande.`);
      }
    });
  }

  private handleComplaintOrder(ref: string): void {
    // Message complet (référence + motif) => création immédiate, sans autre question.
    if (this.tryDirectComplaint(ref)) return;
    if (/recentes?/.test(ref.toLowerCase())) {
      this.orders.getRecent(5).subscribe({
        next: (list) => {
          if (!list.length) {
            this.state = 'idle';
            this.say("Aucune commande récente. Donnez-moi une référence pour continuer.");
            return;
          }
          this.say('Choisissez la commande concernée :', undefined, list.map((o: any) => o.reference));
        },
        error: () => {
          this.state = 'idle';
          this.say("Impossible de charger les commandes. Réessayez plus tard.");
        }
      });
      return;
    }
    this.orders.getByReference(ref).subscribe({
      next: (o: any) => {
        this.pendingOrderId = o.id;
        this.pendingOrderRef = o.reference;
        this.state = 'awaitComplaintReasonText';
        this.say(`C'est noté pour ${o.reference}. Quel est le motif ?`,
          undefined, ['Retard de livraison', 'Produit endommagé', 'Quantité incorrecte', 'Problème de montant', 'Autre (décrire)']);
      },
      error: () => {
        this.say(`Commande « ${ref} » introuvable. Réessayez ou tapez « récentes ».`);
      }
    });
  }

  private handleComplaintReasonText(text: string): void {
    if (/^autre/.test(text.toLowerCase())) {
      this.say('Décrivez le problème en une phrase :');
      return;
    }
    this.pendingReason = text;
    this.state = 'awaitComplaintConfirm';
    this.say(`Je vais ouvrir une réclamation pour ${this.pendingOrderRef} — motif : « ${text} ». Confirmer ?`,
      undefined, ['Oui, confirmer', 'Annuler']);
  }

  private handleComplaintConfirm(norm: string): void {
    if (/^(oui|confirmer|ok|valider)/.test(norm)) {
      if (!this.pendingOrderId || !this.pendingReason) {
        this.reset();
        return;
      }
      const orderId = this.pendingOrderId;
      const ref = this.pendingOrderRef || orderId;
      const reason = this.pendingReason;
      this.createDisputeNow(orderId, ref, reason);
      return;
    }
    this.reset();
  }

  // ── Utilitaires ─────────────────────────────────────────────

  private say(text: string, links?: ChatLink[], options?: string[]): void {
    this.push({ from: 'bot', text, links, options });
  }

  private push(m: ChatMessage): void {
    this.messagesSubject.next([...this.messagesSubject.getValue(), m]);
  }

  private firstName(): string {
    try {
      const u = this.loginService.getCurrentUser();
      return u?.firstName || '';
    } catch {
      return '';
    }
  }

  private roles(): string[] {
    try {
      return this.loginService.getCurrentUser()?.roles || [];
    } catch {
      return [];
    }
  }

  private isShop(): boolean {
    const r = this.roles();
    return r.includes('SHOP_ADMIN') || r.includes('SHOP_AGENT');
  }

  private orderStatusLabel(s: string): string {
    const map: Record<string, string> = {
      DRAFT: 'brouillon', CONFIRMED: 'confirmée', PREPARING: 'en préparation',
      READY_FOR_DELIVERY: 'prête pour livraison', DELIVERY_ACCEPTED: 'livraison acceptée',
      IN_DELIVERY: 'en livraison', DELIVERED: 'livrée', ACCEPTED: 'acceptée',
      CANCELLED: 'annulée', REJECTED: 'rejetée',
    };
    return map[s] || s;
  }
}
