import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { ConfirmDialogService } from './confirm-dialog.service';

/**
 * Popup de confirmation globale (rendue une fois dans le layout).
 * Boutons Valider / Non, fermeture au fond et à Échap (= Non).
 */
@Component({
    selector: 'app-confirm-dialog',
    templateUrl: './confirm-dialog.component.html',
    styleUrls: ['./confirm-dialog.component.css'],
    standalone: false
})
export class ConfirmDialogComponent implements OnInit, OnDestroy {
  visible = false;
  title = '';
  message = '';
  confirmLabel = 'Valider';
  cancelLabel = 'Non';
  danger = false;

  private sub = new Subscription();

  constructor(private dialogs: ConfirmDialogService) {}

  ngOnInit(): void {
    this.sub.add(this.dialogs.state().subscribe(s => {
      this.visible = s.visible;
      this.title = s.title;
      this.message = s.message;
      this.confirmLabel = s.confirmLabel;
      this.cancelLabel = s.cancelLabel;
      this.danger = s.danger;
    }));
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  choose(value: boolean): void {
    if (!this.visible) return;
    this.dialogs.answer(value);
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.choose(false);
  }
}
