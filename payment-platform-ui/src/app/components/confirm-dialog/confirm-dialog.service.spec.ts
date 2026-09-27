// @ts-nocheck
/**
 * Tests du service ConfirmDialogService.
 * Perimetre : ouverture, reponse Valider/Non, une seule emission.
 */
import { ConfirmDialogService } from './confirm-dialog.service';

describe('ConfirmDialogService', () => {
  let service: ConfirmDialogService;

  beforeEach(() => {
    service = new ConfirmDialogService();
  });

  it('should be created hidden', (done) => {
    service.state().subscribe(s => {
      expect(s.visible).toBeFalse();
      done();
    });
  });

  it('should emit true on answer(true)', (done) => {
    service.confirm({ title: 'T', message: 'M' }).subscribe(v => {
      expect(v).toBeTrue();
      done();
    });
    service.answer(true);
  });

  it('should emit false on answer(false)', (done) => {
    service.confirm({ title: 'T', message: 'M' }).subscribe(v => {
      expect(v).toBeFalse();
      done();
    });
    service.answer(false);
  });

  it('should expose custom labels and danger flag', (done) => {
    service.confirm({ title: 'T', message: 'M', confirmLabel: 'Oui', cancelLabel: 'Non', danger: true });
    service.state().subscribe(s => {
      expect(s.visible).toBeTrue();
      expect(s.confirmLabel).toBe('Oui');
      expect(s.cancelLabel).toBe('Non');
      expect(s.danger).toBeTrue();
      done();
    });
  });
});
