import { Injectable } from '@angular/core';

/**
 * Son + vibration à l'arrivée d'une notification (téléphone et desktop).
 * Web Push natif : Android joue déjà le son système en arrière-plan ;
 * ce service couvre le premier plan (SSE + FCM) et la vibration mobile.
 */
@Injectable({ providedIn: 'root' })
export class NotificationSoundService {
  private readonly STORAGE_KEY = 'pp-notif-sound';
  private readonly SOUND_URL = 'assets/sounds/notification-chime.wav';
  private unlocked = false;

  constructor() {
    // Les navigateurs exigent un geste utilisateur avant tout Audio.
    try {
      const unlock = () => {
        this.unlocked = true;
        window.removeEventListener('pointerdown', unlock);
      };
      window.addEventListener('pointerdown', unlock, { once: true });
    } catch {}
  }

  isEnabled(): boolean {
    try {
      return localStorage.getItem(this.STORAGE_KEY) !== 'off';
    } catch {
      return true;
    }
  }

  setEnabled(on: boolean): void {
    try {
      localStorage.setItem(this.STORAGE_KEY, on ? 'on' : 'off');
    } catch {}
  }

  /** Joue le carillon + vibration courte (no-op si désactivé ou non supporté). */
  play(): void {
    if (!this.isEnabled()) return;
    try {
      const nav = navigator as Navigator & { vibrate?: (p: number | number[]) => boolean };
      if (typeof nav.vibrate === 'function') {
        nav.vibrate(200);
      }
    } catch {}
    if (!this.unlocked) return;
    try {
      if (typeof Audio === 'undefined') return;
      const audio = new Audio(this.SOUND_URL);
      audio.volume = 0.7;
      const played = audio.play();
      if (played && typeof played.catch === 'function') {
        played.catch(() => {});
      }
    } catch {}
  }
}
