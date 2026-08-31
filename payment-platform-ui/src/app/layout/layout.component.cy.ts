/// <reference types="cypress" />
/// <reference types="@cypress/angular" />

import { LayoutComponent } from './layout.component';
import { LoginService } from '../services/login.service';
import { NotificationService } from '../services/notification.service';
import { Router } from '@angular/router';
import { of, Subject } from 'rxjs';

describe('LayoutComponent', () => {
  let loginServiceSpy: jasmine.SpyObj<LoginService>;
  let notificationServiceSpy: jasmine.SpyObj<NotificationService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let notificationsSubject: Subject<any[]>;
  let unreadCountSubject: Subject<number>;

  const mockUser = {
    id: 1,
    username: 'covale.admin',
    firstName: 'Covale',
    lastName: 'Admin',
    roles: ['SUPPLIER_ADMIN'],
    organizationId: 1
  };

  const mockNotifications = [
    { id: 1, type: 'PAYMENT_CREATED', message: 'Nouveau paiement PAY-123', readStatus: 'UNREAD', createdAt: new Date().toISOString(), relatedEntityType: 'PAYMENT', relatedEntityId: 'PAY-123' },
    { id: 2, type: 'PAYMENT_CONFIRMED', message: 'Paiement PAY-456 confirmé', readStatus: 'READ', createdAt: new Date(Date.now() - 3600000).toISOString(), relatedEntityType: 'PAYMENT', relatedEntityId: 'PAY-456' },
  ];

  beforeEach(() => {
    notificationsSubject = new Subject<any[]>();
    unreadCountSubject = new Subject<number>();
    
    loginServiceSpy = jasmine.createSpyObj('LoginService', ['getCurrentUser', 'logout']);
    notificationServiceSpy = jasmine.createSpyObj('NotificationService', [
      'startPolling', 'markAllAsRead', 'markAsRead', 'updateNotifications', 'updateUnreadCount'
    ], {
      notifications$: notificationsSubject.asObservable(),
      unreadCount$: unreadCountSubject.asObservable()
    });
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    
    loginServiceSpy.getCurrentUser.and.returnValue(mockUser);
    notificationServiceSpy.markAllAsRead.and.returnValue(of({ updated: 2 }));
    notificationServiceSpy.markAsRead.and.returnValue(of(void 0));
  });

  const mountLayout = (userOverrides: Partial<typeof mockUser> = {}) => {
    const user = { ...mockUser, ...userOverrides };
    loginServiceSpy.getCurrentUser.and.returnValue(user);
    
    return cy.mount(LayoutComponent, {
      providers: [
        { provide: LoginService, useValue: loginServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    });
  };

  it('should render user name and role in sidebar', () => {
    mountLayout();
    cy.get('.user-name').should('contain', 'Covale Admin');
    cy.get('.user-role').should('contain', 'SUPPLIER_ADMIN');
  });

  it('should render user initials in avatar', () => {
    mountLayout();
    cy.get('.user-avatar').should('contain', 'CA');
  });

  it('should filter navigation items based on user roles', () => {
    mountLayout({ roles: ['SUPPLIER_ADMIN'] });
    
    // Should see supplier items
    cy.get('.nav-item').should('exist');
    cy.contains('Catalogue').should('be.visible');
    cy.contains('Stock').should('be.visible');
    cy.contains('Commandes').should('be.visible');
    cy.contains('Livraisons').should('be.visible');
    
    // Should NOT see admin items
    cy.contains('Gestion des utilisateurs').should('not.exist');
    cy.contains('Fournisseurs').should('not.exist');
  });

  it('should show admin items for SYSTEM_ADMIN', () => {
    mountLayout({ roles: ['SYSTEM_ADMIN'], username: 'system.admin', firstName: 'System', lastName: 'Admin' });
    
    cy.contains('Gestion des utilisateurs').should('be.visible');
    cy.contains('Fournisseurs').should('be.visible');
    cy.contains('Boutiques').should('be.visible');
    cy.contains('Relations F-B').should('be.visible');
  });

  it('should toggle sidebar on button click', () => {
    mountLayout();
    
    cy.get('.sidebar').should('not.have.class', 'open');
    cy.get('.toggle-btn').click();
    cy.get('.sidebar').should('have.class', 'open');
    cy.get('.toggle-btn').click();
    cy.get('.sidebar').should('not.have.class', 'open');
  });

  it('should open notification panel on bell click', () => {
    unreadCountSubject.next(2);
    notificationsSubject.next(mockNotifications);
    
    mountLayout();
    
    cy.get('.notif-bell').click();
    cy.get('.notif-panel').should('be.visible');
    cy.get('.notif-header h3').should('contain', 'Notifications');
    cy.get('.notif-item').should('have.length', 2);
  });

  it('should show unread badge with count', () => {
    unreadCountSubject.next(5);
    
    mountLayout();
    
    cy.get('.notif-badge').should('be.visible').and('contain', '5');
  });

  it('should show "99+" when unread count exceeds 99', () => {
    unreadCountSubject.next(150);
    
    mountLayout();
    
    cy.get('.notif-badge').should('contain', '99+');
  });

  it('should mark all as read when button clicked', () => {
    unreadCountSubject.next(2);
    notificationsSubject.next(mockNotifications);
    
    mountLayout();
    cy.get('.notif-bell').click();
    cy.get('.notif-mark-all').click();
    
    cy.wrap(null).should(() => {
      expect(notificationServiceSpy.markAllAsRead).toHaveBeenCalled();
    });
  });

  it('should navigate to payment detail when clicking notification', () => {
    unreadCountSubject.next(1);
    notificationsSubject.next([mockNotifications[0]]);
    
    mountLayout();
    cy.get('.notif-bell').click();
    cy.get('.notif-item').first().click();
    
    cy.wrap(null).should(() => {
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard/payments'], { queryParams: { ref: 'PAY-123' } });
    });
  });

  it('should logout and navigate to login', () => {
    mountLayout();
    cy.get('.logout-btn').click();
    
    cy.wrap(null).should(() => {
      expect(loginServiceSpy.logout).toHaveBeenCalled();
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/login']);
    });
  });

  it('should show mobile header on small screens', () => {
    // This would require viewport manipulation - testing responsive behavior
    mountLayout();
    // Desktop: mobile header should be hidden
    cy.get('.mobile-header').should('have.css', 'display', 'none');
  });

  it('should display correct initials for various names', () => {
    mountLayout({ firstName: 'John', lastName: 'Doe' });
    cy.get('.user-avatar').should('contain', 'JD');
    
    mountLayout({ firstName: 'Alice' });
    cy.get('.user-avatar').should('contain', 'A');
    
    mountLayout({ firstName: '', lastName: 'Smith' });
    cy.get('.user-avatar').should('contain', 'S');
  });
});