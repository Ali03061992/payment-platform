// @ts-nocheck
import { StatusLabelPipe } from './status-label.pipe';

describe('StatusLabelPipe', () => {
  let pipe: StatusLabelPipe;

  beforeEach(() => {
    pipe = new StatusLabelPipe();
  });

  it('should create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  describe('transform', () => {
    it('should transform PENDING to En attente', () => {
      expect(pipe.transform('PENDING')).toBe('En attente');
    });

    it('should transform DRAFT to Brouillon', () => {
      expect(pipe.transform('DRAFT')).toBe('Brouillon');
    });

    it('should transform CONFIRMED to Confirmé', () => {
      expect(pipe.transform('CONFIRMED')).toBe('Confirmé');
    });

    it('should transform PREPARING to En préparation', () => {
      expect(pipe.transform('PREPARING')).toBe('En préparation');
    });

    it('should transform READY_FOR_DELIVERY to Prêt pour livraison', () => {
      expect(pipe.transform('READY_FOR_DELIVERY')).toBe('Prêt pour livraison');
    });

    it('should transform IN_DELIVERY to En livraison', () => {
      expect(pipe.transform('IN_DELIVERY')).toBe('En livraison');
    });

    it('should transform DELIVERED to Livré', () => {
      expect(pipe.transform('DELIVERED')).toBe('Livré');
    });

    it('should transform ACCEPTED to Accepté', () => {
      expect(pipe.transform('ACCEPTED')).toBe('Accepté');
    });

    it('should transform CANCELLED to Annulé', () => {
      expect(pipe.transform('CANCELLED')).toBe('Annulé');
    });

    it('should transform REJECTED to Rejeté', () => {
      expect(pipe.transform('REJECTED')).toBe('Rejeté');
    });

    it('should transform DELIVERY_REJECTED to Livraison rejetée', () => {
      expect(pipe.transform('DELIVERY_REJECTED')).toBe('Livraison rejetée');
    });

    it('should return raw value for unknown status', () => {
      expect(pipe.transform('UNKNOWN')).toBe('UNKNOWN');
    });

    it('should return empty string for empty input', () => {
      expect(pipe.transform('')).toBe('');
    });
  });
});
