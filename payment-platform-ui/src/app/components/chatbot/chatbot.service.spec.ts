// @ts-nocheck
/**
 * Tests du ChatbotService : processus conversationnel équilibré.
 * Perimetre : start/reset, intentions FR/EN/AR, suivi commande, paiements,
 * réclamation guidée (directe + étapes), statuts, garde-fous.
 * Moyens : TestBed, stubs jasmine, TranslateService stub FR.
 */
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { TranslateService } from '@ngx-translate/core';
import { ChatbotService } from './chatbot.service';
import { OrderService } from '../../services/order.service';
import { DisputeService } from '../../services/dispute.service';
import { PaymentService } from '../../services/payment.service';
import { LoginService } from '../../services/login.service';

const FR: Record<string, string> = {
  'CHATBOT.HELLO': 'Bonjour',
  'CHATBOT.HELLO_SHORT': 'Bonjour',
  'CHATBOT.WHAT_ELSE': 'Que puis-je faire',
  'CHATBOT.OPT_TRACK': 'Suivre une commande',
  'CHATBOT.OPT_COMPLAINT': 'Déposer une réclamation',
  'CHATBOT.OPT_PAY': 'Aide paiements',
  'CHATBOT.OPT_DELIVERY': 'Aide livraisons',
  'CHATBOT.OPT_YES_SHOW': 'Oui, montrer',
  'CHATBOT.OPT_NO_THANKS': 'Non merci',
  'CHATBOT.OPT_YES_CONFIRM': 'Oui, confirmer',
  'CHATBOT.OPT_CANCEL': 'Annuler',
  'CHATBOT.ASK_ORDER_REF': 'référence de la commande',
  'CHATBOT.PAY_HELP': 'Côté paiements',
  'CHATBOT.DELIVERY_HELP': 'Une livraison passe par',
  'CHATBOT.PLEASURE': 'Avec plaisir',
  'CHATBOT.HUMAN': 'cas particulier',
  'CHATBOT.NOT_UNDERSTOOD': 'pas bien compris',
  'CHATBOT.NO_RECENT': 'Aucune commande récente trouvée',
  'CHATBOT.NO_RECENT_SHORT': 'Aucune commande récente.',
  'CHATBOT.RECENT_LIST': 'dernières commandes',
  'CHATBOT.LOAD_ERROR': 'Impossible de charger',
  'CHATBOT.ORDER_FOUND': 'Commande',
  'CHATBOT.ORDER_NOT_FOUND': 'introuvable',
  'CHATBOT.ORDER_NOT_FOUND_RETRY': 'introuvable',
  'CHATBOT.NO_PENDING': 'aucun paiement en attente',
  'CHATBOT.PENDING_SOME': 'paiement(s) en attente',
  'CHATBOT.PAYMENTS_ERROR': 'Impossible de charger les paiements',
  'CHATBOT.COMPLAINT_START': 'Décrivez le problème',
  'CHATBOT.DISPUTE_CREATED': 'Réclamation enregistrée',
  'CHATBOT.DISPUTE_FAILED': "Échec de l'enregistrement",
  'CHATBOT.CHOOSE_ORDER': 'Choisissez',
  'CHATBOT.ASK_REASON': 'Quel est le motif',
  'CHATBOT.REASON_LATE': 'Retard de livraison',
  'CHATBOT.REASON_DAMAGED': 'Produit endommagé',
  'CHATBOT.REASON_QTY': 'Quantité incorrecte',
  'CHATBOT.REASON_AMOUNT': 'Problème de montant',
  'CHATBOT.REASON_OTHER': 'Autre (décrire)',
  'CHATBOT.DESCRIBE_ONE_SENTENCE': 'Décrivez le problème en une phrase',
  'CHATBOT.CONFIRM_COMPLAINT': 'Confirmer ?',
  'CHATBOT.GENERIC_ERROR': 'erreur',
  'CHATBOT.EXAMPLE_PREFIX': 'ex.',
  'CHATBOT.LINK_DETAIL': 'Voir le détail',
  'CHATBOT.LINK_DISPUTE': 'Voir la réclamation',
  'CHATBOT.LINK_PAYMENTS': 'Voir les paiements',
  'STATUS.DELIVERED': 'Livré',
};

describe('ChatbotService', () => {
  let service: ChatbotService;
  let orders: jasmine.SpyObj<OrderService>;
  let disputes: jasmine.SpyObj<DisputeService>;
  let payments: jasmine.SpyObj<PaymentService>;
  let login: jasmine.SpyObj<LoginService>;

  function lastBotText(): string {
    const msgs = (service as any).messagesSubject.getValue();
    return msgs.filter((m: any) => m.from === 'bot').pop()?.text || '';
  }

  function botCount(): number {
    return (service as any).messagesSubject.getValue().filter((m: any) => m.from === 'bot').length;
  }

  beforeEach(() => {
    const ordersSpy = jasmine.createSpyObj('OrderService', ['getByReference', 'getRecent']);
    const disputesSpy = jasmine.createSpyObj('DisputeService', ['create']);
    const paymentsSpy = jasmine.createSpyObj('PaymentService', ['list']);
    const loginSpy = jasmine.createSpyObj('LoginService', ['getCurrentUser']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const translateSpy = jasmine.createSpyObj('TranslateService', ['instant']);
    translateSpy.instant.and.callFake((key: string) => FR[key] || key);
    loginSpy.getCurrentUser.and.returnValue({ firstName: 'Ali', roles: ['SHOP_ADMIN'] } as any);

    TestBed.configureTestingModule({
      imports: [],
      providers: [
        ChatbotService,
        { provide: OrderService, useValue: ordersSpy },
        { provide: DisputeService, useValue: disputesSpy },
        { provide: PaymentService, useValue: paymentsSpy },
        { provide: LoginService, useValue: loginSpy },
        { provide: Router, useValue: routerSpy },
        { provide: TranslateService, useValue: translateSpy }
      ]
    });
    service = TestBed.inject(ChatbotService);
    orders = TestBed.inject(OrderService) as jasmine.SpyObj<OrderService>;
    disputes = TestBed.inject(DisputeService) as jasmine.SpyObj<DisputeService>;
    payments = TestBed.inject(PaymentService) as jasmine.SpyObj<PaymentService>;
    login = TestBed.inject(LoginService) as jasmine.SpyObj<LoginService>;
  });

  describe('start/reset', () => {
    it('should greet once with 4 options', () => {
      service.start();
      service.start();
      expect(botCount()).toBe(1);
      expect(lastBotText()).toContain('Bonjour');
      expect((service as any).messagesSubject.getValue()[0].options.length).toBe(4);
    });

    it('should reset state and pending data', () => {
      service.start();
      (service as any).state = 'awaitComplaintConfirm';
      (service as any).pendingOrderId = 'x';
      service.reset();
      expect((service as any).state).toBe('idle');
      expect((service as any).pendingOrderId).toBeNull();
      expect(lastBotText()).toContain('Que puis-je faire');
    });

    it('should ignore empty input', () => {
      service.start();
      const n = botCount();
      service.send('   ');
      expect(botCount()).toBe(n);
    });
  });

  describe('intentions', () => {
    beforeEach(() => service.start());

    it('should route suivi commande vers ASK_ORDER_REF', () => {
      service.send('je veux suivre ma commande');
      expect(lastBotText()).toContain('référence de la commande');
      expect((service as any).state).toBe('awaitOrderRef');
    });

    it('should route reclamation vers complaint', () => {
      service.send('je veux faire une réclamation');
      expect((service as any).state).toBe('awaitComplaintOrder');
    });

    it('should route aide paiements (FR) vers pending', () => {
      service.send('aide paiements');
      expect((service as any).state).toBe('awaitPendingChoice');
      expect(lastBotText()).toContain('Côté paiements');
    });

    it('should route AR payment option vers pending (non-régression)', () => {
      service.send('مساعدة المدفوعات');
      expect((service as any).state).toBe('awaitPendingChoice');
    });

    it('should route AR delivery option vers delivery help', () => {
      service.send('مساعدة التسليم');
      expect(lastBotText()).toContain('Une livraison passe par');
    });

    it('should route merci vers pleasure', () => {
      service.send('merci beaucoup');
      expect(lastBotText()).toContain('Avec plaisir');
    });

    it('should fallback sur incomprehension', () => {
      service.send('blabla xyz');
      expect(lastBotText()).toContain('pas bien compris');
      expect((service as any).state).toBe('idle');
    });
  });

  describe('suivi commande', () => {
    beforeEach(() => {
      service.start();
      service.send('suivre commande');
    });

    it('should accept reference accentuee « récentes » (non-régression)', () => {
      orders.getRecent.and.returnValue(of([]));
      service.send('récentes');
      expect(orders.getRecent).toHaveBeenCalledWith(5);
    });

    it('should list recent orders as options', () => {
      orders.getRecent.and.returnValue(of([{ reference: 'ORD-1', status: 'DELIVERED' }]));
      service.send('recentes');
      const last = (service as any).messagesSubject.getValue().filter((m: any) => m.from === 'bot').pop();
      expect(last.text).toContain('dernières commandes');
      expect(last.options).toContain('ORD-1');
    });

    it('should show order detail on lookup success', () => {
      orders.getByReference.and.returnValue(of({ id: '1', reference: 'ORD-1', status: 'DELIVERED', total: 100, currency: 'TND' }));
      service.send('ORD-1');
      expect(orders.getByReference).toHaveBeenCalledWith('ORD-1');
      expect(lastBotText()).toContain('Commande');
      expect((service as any).state).toBe('idle');
    });

    it('should stay in awaitOrderRef on lookup error', () => {
      orders.getByReference.and.returnValue(throwError(() => new Error('x')));
      service.send('ORD-XXX');
      expect(lastBotText()).toContain('introuvable');
    });
  });

  describe('paiements en attente', () => {
    beforeEach(() => {
      service.start();
      service.send('aide paiements');
    });

    it('should list pending payments on yes', () => {
      payments.list.and.returnValue(of([]));
      service.send('Oui, montrer');
      expect(payments.list).toHaveBeenCalledWith(0, 50);
      expect(lastBotText()).toContain('aucun paiement en attente');
    });

    it('should reset on no', () => {
      service.send('Non merci');
      expect((service as any).state).toBe('idle');
      expect(payments.list).not.toHaveBeenCalled();
    });
  });

  describe('réclamation guidée', () => {
    it('should create directly on ref+motif (zero question)', () => {
      service.start();
      orders.getByReference.and.returnValue(of({ id: 'o1', reference: 'ORD-A1B2C3' }));
      disputes.create.and.returnValue(of({ id: 'd1' }));
      service.send('litige');
      service.send('colis cassé ORD-A1B2C3');
      expect(disputes.create).toHaveBeenCalled();
      expect(lastBotText()).toContain('Réclamation enregistrée');
    });

    it('should follow reason flow then confirm', () => {
      service.start();
      orders.getByReference.and.returnValue(of({ id: 'o1', reference: 'ORD-9' }));
      disputes.create.and.returnValue(of({ id: 'd1' }));
      service.send('litige');
      service.send('ORD-9');
      expect((service as any).state).toBe('awaitComplaintReasonText');
      service.send('Retard de livraison');
      expect((service as any).state).toBe('awaitComplaintConfirm');
      service.send('oui');
      expect(disputes.create).toHaveBeenCalledWith({ orderId: 'o1', reason: 'Retard de livraison' });
    });

    it('should ask describe on autre (FR/EN/AR)', () => {
      service.start();
      orders.getByReference.and.returnValue(of({ id: 'o1', reference: 'ORD-9' }));
      service.send('litige');
      service.send('ORD-9');
      service.send('أخرى (صِف)');
      expect(lastBotText()).toContain('une phrase');
      expect((service as any).state).toBe('awaitComplaintReasonText');
    });

    it('should reset on cancel at confirm', () => {
      service.start();
      orders.getByReference.and.returnValue(of({ id: 'o1', reference: 'ORD-9' }));
      service.send('litige');
      service.send('ORD-9');
      service.send('Retard de livraison');
      service.send('annuler');
      expect(disputes.create).not.toHaveBeenCalled();
      expect((service as any).state).toBe('idle');
    });

    it('should handle dispute creation error without blocking', () => {
      service.start();
      orders.getByReference.and.returnValue(of({ id: 'o1', reference: 'ORD-9ABC' }));
      disputes.create.and.returnValue(throwError(() => ({ error: {} })));
      service.send('litige');
      service.send('emballage déchiré ORD-9ABC');
      expect(lastBotText()).toContain("Échec de l'enregistrement");
      expect((service as any).state).toBe('idle');
    });
  });

  describe('utilitaires', () => {
    it('should fallback status code inconnu', () => {
      service.start();
      expect((service as any).orderStatusLabel('UNKNOWN_XYZ')).toBe('UNKNOWN_XYZ');
    });
  });
});
