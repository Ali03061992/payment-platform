import './commands';

Cypress.on('uncaught:exception', (err) => {
  if (err.message.includes('ResizeObserver loop')) {
    return false;
  }
  return false;
});

Cypress.on('window:before:load', (win) => {
  try {
    win.localStorage.setItem('onboarding_completed', 'true');
    win.localStorage.setItem('notification_choice', 'dismissed');
  } catch {
    // ignore storage errors in edge cases
  }
});

beforeEach(() => {
  cy.window({ log: false }).then((win) => {
    try {
      win.localStorage.setItem('onboarding_completed', 'true');
      win.localStorage.setItem('notification_choice', 'dismissed');
    } catch {
      // ignore
    }
  });
  cy.get('body', { log: false }).then(($body) => {
    const skip = $body.find('.tour-tooltip .tour-btn-skip');
    if (skip.length) {
      cy.wrap(skip.first(), { log: false }).click({ force: true });
    }
    const later = $body.find('.notification-banner .banner-btn-dismiss');
    if (later.length) {
      cy.wrap(later.first(), { log: false }).click({ force: true });
    }
  });
});
