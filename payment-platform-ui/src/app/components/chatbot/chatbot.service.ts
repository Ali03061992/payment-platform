import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { DisputeService } from '../../services/dispute.service';
import { LoginService } from '../../services/login.service';
import { OrderService } from '../../services/order.service';
import { PaymentService } from '../../services/payment.service';
import { TranslateService } from '@ngx-translate/core';

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
    private loginService: LoginService,
    private translate: TranslateService
  ) {}

  start(): void {
    if (this.started) return;
    this.started = true;
    this.say(
      this.translate.instant('CHATBOT.HELLO', { name: this.firstName() }),
      undefined,
      [this.translate.instant('CHATBOT.OPT_TRACK'), this.translate.instant('CHATBOT.OPT_COMPLAINT'), this.translate.instant('CHATBOT.OPT_PAY'), this.translate.instant('CHATBOT.OPT_DELIVERY')]
    );
  }

  reset(): void {
    this.state = 'idle';
    this.pendingOrderId = null;
    this.pendingOrderRef = null;
    this.pendingReason = null;
    this.say(this.translate.instant('CHATBOT.WHAT_ELSE'),
      undefined, [this.translate.instant('CHATBOT.OPT_TRACK'), this.translate.instant('CHATBOT.OPT_COMPLAINT'), this.translate.instant('CHATBOT.OPT_PAY'), this.translate.instant('CHATBOT.OPT_DELIVERY')]);
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
    if (/(bonjour|salut|hello|coucou|bonsoir|good morning|good afternoon|evening|مرحبا|صباح|مساء|سلام)/.test(norm)) {
      this.say(this.translate.instant('CHATBOT.HELLO_SHORT', { name: this.firstName() }), undefined,
        [this.translate.instant('CHATBOT.OPT_TRACK'), this.translate.instant('CHATBOT.OPT_COMPLAINT'), this.translate.instant('CHATBOT.OPT_PAY'), this.translate.instant('CHATBOT.OPT_DELIVERY')]);
      return;
    }
    if (/(commande|suivi|suivre|statut.*commande|ou en est|order|tracking|track|طلب|تتبع|فين)/.test(norm)) {
      this.state = 'awaitOrderRef';
      this.say(this.translate.instant('CHATBOT.ASK_ORDER_REF'));
      return;
    }
    if (/(reclamation|litige|probleme|plainte|sav|retour|dispute|complaint|شكوى|نزاع|مشكل)/.test(norm)) {
      this.startComplaint();
      return;
    }
    if (/(paiement|payer|facture|balance|solde|payment|pay|invoice|خلاص|دفع|مدفوع|فاتورة|رصيد)/.test(norm)) {
      this.state = 'awaitPendingChoice';
      this.say(
        this.translate.instant('CHATBOT.PAY_HELP'),
        this.isShop()
          ? [{ label: this.translate.instant('CHATBOT.LINK_BALANCE'), route: '/dashboard/shop/balance' }]
          : [{ label: this.translate.instant('CHATBOT.LINK_PAYMENTS'), route: '/dashboard/payments' }],
        [this.translate.instant('CHATBOT.OPT_YES_SHOW'), this.translate.instant('CHATBOT.OPT_NO_THANKS')]
      );
      return;
    }
    if (/(livraison|livreur|colis|recu|reception|delivery|driver|package|received|توصيل|تسليم|استلام)/.test(norm)) {
      this.say(
        this.translate.instant('CHATBOT.DELIVERY_HELP'),
        this.isShop()
          ? [{ label: this.translate.instant('CHATBOT.LINK_MY_DELIVERIES'), route: '/dashboard/shop/deliveries' }]
          : [{ label: this.translate.instant('CHATBOT.LINK_DELIVERIES'), route: '/dashboard/supplier/deliveries' }],
        [this.translate.instant('CHATBOT.OPT_TRACK'), this.translate.instant('CHATBOT.OPT_COMPLAINT')]
      );
      return;
    }
    if (/(merci|thanks|thank you|شكرا)/.test(norm)) {
      this.say(this.translate.instant('CHATBOT.PLEASURE'),
        undefined, [this.translate.instant('CHATBOT.OPT_TRACK'), this.translate.instant('CHATBOT.OPT_COMPLAINT')]);
      return;
    }
    if (/(humain|conseiller|contact|telephone|email|aide|human|agent|help|بشري|مستشار|اتصال|مساعدة)/.test(norm)) {
      this.say(this.translate.instant('CHATBOT.HUMAN'),
        undefined, [this.translate.instant('CHATBOT.OPT_COMPLAINT')]);
      return;
    }
    this.say(this.translate.instant('CHATBOT.NOT_UNDERSTOOD', { raw }),
      undefined, [this.translate.instant('CHATBOT.OPT_TRACK'), this.translate.instant('CHATBOT.OPT_COMPLAINT'), this.translate.instant('CHATBOT.OPT_PAY'), this.translate.instant('CHATBOT.OPT_DELIVERY')]);
  }

  // ── Suivi commande ──────────────────────────────────────────

  private handleOrderRef(ref: string): void {
    const normRef = ref.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    if (/recentes?/.test(normRef)) {
      this.orders.getRecent(5).subscribe({
        next: (list) => {
          if (!list.length) {
            this.state = 'idle';
            this.say(this.translate.instant('CHATBOT.NO_RECENT'));
            return;
          }
          this.state = 'awaitOrderRef';
          this.say(this.translate.instant('CHATBOT.RECENT_LIST'),
            list.map((o: any) => ({ label: `${o.reference} — ${this.orderStatusLabel(o.status)}`, route: '' })),
            list.map((o: any) => o.reference));
        },
        error: () => {
          this.state = 'idle';
          this.say(this.translate.instant('CHATBOT.LOAD_ERROR'));
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
        this.say(this.translate.instant('CHATBOT.ORDER_FOUND', { ref: o.reference, status: this.orderStatusLabel(o.status), total: Number(o.total).toFixed(2), currency: o.currency || 'TND' }),
          [{ label: this.translate.instant('CHATBOT.LINK_DETAIL'), route: detailRoute }],
          [this.translate.instant('CHATBOT.OPT_TRACK'), this.translate.instant('CHATBOT.OPT_COMPLAINT')]);
      },
      error: () => {
        this.say(this.translate.instant('CHATBOT.ORDER_NOT_FOUND', { ref }));
      }
    });
  }

  // ── Paiements en attente (lookup réel) ────────────────────────

  private handlePendingChoice(norm: string): void {
    if (!/^(oui|montrer|ok|yes|show|نعم)/.test(norm)) {
      this.reset();
      return;
    }
    this.payments.list(0, 50).subscribe({
      next: (res: any) => {
        const arr: any[] = Array.isArray(res) ? res : res?.items || [];
        const pending = arr.filter((p) => p.status === 'PENDING');
        this.state = 'idle';
        if (!pending.length) {
          this.say(this.translate.instant('CHATBOT.NO_PENDING'),
            [{ label: this.translate.instant('CHATBOT.LINK_PAYMENTS'), route: '/dashboard/payments' }],
            [this.translate.instant('CHATBOT.OPT_TRACK'), this.translate.instant('CHATBOT.OPT_COMPLAINT')]);
          return;
        }
        const total = pending.reduce((s, p) => s + Number(p.amount || 0), 0);
        const refs = pending.slice(0, 3).map((p) => p.reference).join(', ');
        this.say(this.translate.instant('CHATBOT.PENDING_SOME', { n: pending.length, total: total.toFixed(2), refs: refs ? ` (${this.translate.instant('CHATBOT.EXAMPLE_PREFIX')} ${refs})` : '' }),
          [{ label: this.translate.instant('CHATBOT.LINK_PAYMENTS'), route: '/dashboard/payments' }],
          [this.translate.instant('CHATBOT.OPT_TRACK'), this.translate.instant('CHATBOT.OPT_COMPLAINT')]);
      },
      error: () => {
        this.state = 'idle';
        this.say(this.translate.instant('CHATBOT.PAYMENTS_ERROR'),
          [{ label: this.translate.instant('CHATBOT.LINK_PAYMENTS'), route: '/dashboard/payments' }],
          [this.translate.instant('CHATBOT.OPT_TRACK'), this.translate.instant('CHATBOT.OPT_COMPLAINT')]);
      }
    });
  }

  // ── Réclamation guidée ──────────────────────────────────────

  private startComplaint(): void {
    this.state = 'awaitComplaintOrder';
    this.say(this.translate.instant('CHATBOT.COMPLAINT_START'));
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
        this.say(this.translate.instant('CHATBOT.ORDER_NOT_FOUND', { ref: m[0] }));
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
        this.say(this.translate.instant('CHATBOT.DISPUTE_CREATED', { ref, reason }),
          [{ label: this.translate.instant('CHATBOT.LINK_DISPUTE'), route: detailRoute }],
          [this.translate.instant('CHATBOT.OPT_TRACK'), this.translate.instant('CHATBOT.OPT_COMPLAINT')]);
      },
      error: (e: any) => {
        this.state = 'idle';
        this.say(this.translate.instant('CHATBOT.DISPUTE_FAILED', { err: e.error?.message || this.translate.instant('CHATBOT.GENERIC_ERROR') }));
      }
    });
  }

  private handleComplaintOrder(ref: string): void {
    // Message complet (référence + motif) => création immédiate, sans autre question.
    if (this.tryDirectComplaint(ref)) return;
    if (/recentes?/.test(ref.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, ''))) {
      this.orders.getRecent(5).subscribe({
        next: (list) => {
          if (!list.length) {
            this.state = 'idle';
            this.say(this.translate.instant('CHATBOT.NO_RECENT_SHORT'));
            return;
          }
          this.say(this.translate.instant('CHATBOT.CHOOSE_ORDER'), undefined, list.map((o: any) => o.reference));
        },
        error: () => {
          this.state = 'idle';
          this.say(this.translate.instant('CHATBOT.LOAD_ERROR'));
        }
      });
      return;
    }
    this.orders.getByReference(ref).subscribe({
      next: (o: any) => {
        this.pendingOrderId = o.id;
        this.pendingOrderRef = o.reference;
        this.state = 'awaitComplaintReasonText';
        this.say(this.translate.instant('CHATBOT.ASK_REASON', { ref: o.reference }),
          undefined, [this.translate.instant('CHATBOT.REASON_LATE'), this.translate.instant('CHATBOT.REASON_DAMAGED'), this.translate.instant('CHATBOT.REASON_QTY'), this.translate.instant('CHATBOT.REASON_AMOUNT'), this.translate.instant('CHATBOT.REASON_OTHER')]);
      },
      error: () => {
        this.say(this.translate.instant('CHATBOT.ORDER_NOT_FOUND_RETRY', { ref }));
      }
    });
  }

  private handleComplaintReasonText(text: string): void {
    if (/^(autre|other|أخرى|اخرى)/.test(text.toLowerCase())) {
      this.say(this.translate.instant('CHATBOT.DESCRIBE_ONE_SENTENCE'));
      return;
    }
    this.pendingReason = text;
    this.state = 'awaitComplaintConfirm';
    this.say(this.translate.instant('CHATBOT.CONFIRM_COMPLAINT', { ref: this.pendingOrderRef, reason: text }),
      undefined, [this.translate.instant('CHATBOT.OPT_YES_CONFIRM'), this.translate.instant('CHATBOT.OPT_CANCEL')]);
  }

  private handleComplaintConfirm(norm: string): void {
    if (/^(oui|confirmer|ok|valider|yes|confirm|نعم)/.test(norm)) {
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
    const key = 'STATUS.' + s;
    const v = this.translate.instant(key);
    return v && v !== key ? v : (this.STATUS_FALLBACK[s] || s);
  }

  private readonly STATUS_FALLBACK: Record<string, string> = {
    DRAFT: 'brouillon', CONFIRMED: 'confirmée', PREPARING: 'en préparation',
    READY_FOR_DELIVERY: 'prête pour livraison', DELIVERY_ACCEPTED: 'livraison acceptée',
    IN_DELIVERY: 'en livraison', DELIVERED: 'livrée', ACCEPTED: 'acceptée',
    CANCELLED: 'annulée', REJECTED: 'rejetée',
  };
}
