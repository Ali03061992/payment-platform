import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject, take } from 'rxjs';

export interface ConfirmDialogOptions {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  danger?: boolean;
}

interface ConfirmDialogState extends Required<Omit<ConfirmDialogOptions, 'danger'>> {
  danger: boolean;
  visible: boolean;
}

/**
 * Popup de confirmation (remplace les alert() natives) : Valider / Non.
 * Le composant global (layout) affiche la demande, l'appelant reçoit true/false.
 */
@Injectable({ providedIn: 'root' })
export class ConfirmDialogService {
  private readonly state$ = new BehaviorSubject<ConfirmDialogState>({
    title: '', message: '', confirmLabel: 'Valider', cancelLabel: 'Non',
    danger: false, visible: false,
  });
  private answer$ = new Subject<boolean>();

  /** État observé par le composant de popup. */
  state(): Observable<ConfirmDialogState> {
    return this.state$.asObservable();
  }

  /** Ouvre la popup et émet true (Valider) ou false (Non / fond / Échap). */
  confirm(options: ConfirmDialogOptions): Observable<boolean> {
    this.answer$ = new Subject<boolean>();
    this.state$.next({
      title: options.title,
      message: options.message,
      confirmLabel: options.confirmLabel ?? 'Valider',
      cancelLabel: options.cancelLabel ?? 'Non',
      danger: options.danger ?? false,
      visible: true,
    });
    return this.answer$.pipe(take(1));
  }

  /** Ferme la popup avec la réponse donnée. */
  answer(value: boolean): void {
    this.state$.next({ ...this.state$.value, visible: false });
    this.answer$.next(value);
    this.answer$.complete();
  }
}
