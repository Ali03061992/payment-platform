// @ts-nocheck
/**
 * Tests NotificationSoundService : préférence + lecture/vibration gardées.
 */
import { TestBed } from '@angular/core/testing';
import { NotificationSoundService } from './notification-sound.service';

describe('NotificationSoundService', () => {
  let service: NotificationSoundService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [NotificationSoundService] });
    service = TestBed.inject(NotificationSoundService);
  });

  afterEach(() => localStorage.clear());

  it('should be created, sound on by default', () => {
    expect(service).toBeTruthy();
    expect(service.isEnabled()).toBeTrue();
  });

  it('should persist on/off preference', () => {
    service.setEnabled(false);
    expect(service.isEnabled()).toBeFalse();
    service.setEnabled(true);
    expect(service.isEnabled()).toBeTrue();
  });

  it('should not throw when disabled', () => {
    service.setEnabled(false);
    expect(() => service.play()).not.toThrow();
  });

  it('should attempt playback when enabled', () => {
    const fakeAudio = { volume: 0, play: jasmine.createSpy('play').and.returnValue(Promise.resolve()) };
    spyOn(window as any, 'Audio').and.returnValue(fakeAudio);
    service.setEnabled(true);
    expect(() => service.play()).not.toThrow();
  });
});
