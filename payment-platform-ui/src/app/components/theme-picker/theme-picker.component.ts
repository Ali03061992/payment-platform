import { Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { ThemeService } from '../../services/theme.service';

interface ThemeColor {
  id: string;
  hex: string;
  labelKey: string;
}

@Component({
  selector: 'app-theme-picker',
  template: `
    <div class="theme-picker" (click)="$event.stopPropagation()">
      <h3 class="tp-title">{{ 'SETTINGS.TITLE' | translate }}</h3>
      <div class="tp-group">
        <span class="tp-label">{{ 'SETTINGS.ACCENT1' | translate }}</span>
        <div class="tp-custom">
          <input type="color" [value]="accent1" (change)="applyCustomPrimary($any($event.target).value)" [attr.aria-label]="'SETTINGS.CUSTOM_COLOR' | translate">
          <input type="text" [(ngModel)]="customPrimary" name="customPrimary" class="tp-hex" maxlength="7"
            [placeholder]="'SETTINGS.HEX_PLACEHOLDER' | translate" [attr.aria-label]="'SETTINGS.CUSTOM_COLOR' | translate"
            (change)="applyCustomPrimary(customPrimary)">
        </div>
        <div class="tp-swatches" role="radiogroup" [attr.aria-label]="'SETTINGS.ACCENT1' | translate">
          @for (c of colors; track c.id) {
            <button
              type="button"
              class="tp-swatch"
              role="radio"
              [class.selected]="accent1 === c.hex"
              [attr.aria-checked]="accent1 === c.hex"
              [attr.aria-label]="c.labelKey | translate"
              [attr.title]="c.labelKey | translate"
              [style.background]="c.hex"
              (click)="selectPrimary(c.hex)">
              @if (accent1 === c.hex) {
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="20 6 9 17 4 12"/></svg>
              }
            </button>
          }
        </div>
      </div>
      <div class="tp-group">
        <span class="tp-label">{{ 'SETTINGS.ACCENT2' | translate }}</span>
        <div class="tp-custom">
          <input type="color" [value]="accent2" (change)="applyCustomSecondary($any($event.target).value)" [attr.aria-label]="'SETTINGS.CUSTOM_COLOR' | translate">
          <input type="text" [(ngModel)]="customSecondary" name="customSecondary" class="tp-hex" maxlength="7"
            [placeholder]="'SETTINGS.HEX_PLACEHOLDER' | translate" [attr.aria-label]="'SETTINGS.CUSTOM_COLOR' | translate"
            (change)="applyCustomSecondary(customSecondary)">
        </div>
        <div class="tp-swatches" role="radiogroup" [attr.aria-label]="'SETTINGS.ACCENT2' | translate">
          @for (c of colors; track c.id) {
            <button
              type="button"
              class="tp-swatch"
              role="radio"
              [class.selected]="accent2 === c.hex"
              [attr.aria-checked]="accent2 === c.hex"
              [attr.aria-label]="c.labelKey | translate"
              [attr.title]="c.labelKey | translate"
              [style.background]="c.hex"
              (click)="selectSecondary(c.hex)">
              @if (accent2 === c.hex) {
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="20 6 9 17 4 12"/></svg>
              }
            </button>
          }
        </div>
      </div>
      <button type="button" class="tp-reset" (click)="reset()">{{ 'SETTINGS.RESET' | translate }}</button>
    </div>
  `,
  styles: [`
    .theme-picker { display: flex; flex-direction: column; gap: 12px; padding: 12px; border: 1px solid var(--border-color); border-radius: 12px; background: var(--card-bg); min-width: 260px; }
    .tp-title { margin: 0; font-size: 14px; font-weight: 700; color: var(--text-primary); }
    .tp-group { display: flex; flex-direction: column; gap: 8px; }
    .tp-label { font-size: 12px; font-weight: 600; color: var(--text-secondary); }
    .tp-swatches { display: flex; gap: 8px; flex-wrap: wrap; }
    .tp-custom { display: flex; gap: 8px; align-items: center; }
    .tp-custom input[type="color"] { width: 44px; height: 32px; border: 1px solid var(--border-color); border-radius: 8px; padding: 2px; background: var(--card-bg); cursor: pointer; }
    .tp-hex { flex: 1; min-height: 32px; padding: 4px 8px; border: 1px solid var(--border-color); border-radius: 8px; background: var(--card-bg); color: var(--text-primary); font-size: 12px; font-family: monospace; }
    .tp-swatch { width: 44px; height: 44px; border-radius: 999px; border: 2px solid transparent; outline: 1px solid var(--border-color); outline-offset: 2px; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; padding: 0; transition: transform 0.15s; }
    .tp-swatch:hover { transform: scale(1.08); }
    .tp-swatch.selected { border-color: #fff; outline: 2px solid var(--sky-dark); }
    .tp-swatch:focus-visible { outline: 3px solid var(--sky); outline-offset: 2px; }
    .tp-reset { align-self: flex-start; min-height: 44px; padding: 8px 14px; border-radius: 8px; border: 1px solid var(--border-color); background: var(--bg-tertiary); color: var(--text-primary); font-size: 12px; font-weight: 600; cursor: pointer; }
    .tp-reset:hover { background: var(--hover-bg); }
  `],
  standalone: false
})
export class ThemePickerComponent implements OnInit, OnDestroy {
  accent1 = '#12243a';
  accent2 = '#18a999';
  customPrimary = '';
  customSecondary = '';
  readonly colors: ThemeColor[] = [
    { id: 'marine', hex: '#12243a', labelKey: 'SETTINGS.COLOR_MARINE' },
    { id: 'turquoise', hex: '#18a999', labelKey: 'SETTINGS.COLOR_TURQUOISE' },
    { id: 'ciel', hex: '#0284c7', labelKey: 'SETTINGS.COLOR_SKY' },
    { id: 'rouge', hex: '#e63946', labelKey: 'SETTINGS.COLOR_ROUGE' },
    { id: 'noir', hex: '#1a1a2e', labelKey: 'SETTINGS.COLOR_NOIR' },
    { id: 'vert', hex: '#2e7d32', labelKey: 'SETTINGS.COLOR_VERT' },
    { id: 'violet', hex: '#7b1fa2', labelKey: 'SETTINGS.COLOR_VIOLET' },
    { id: 'orange', hex: '#e65100', labelKey: 'SETTINGS.COLOR_ORANGE' },
    { id: 'sarcelle', hex: '#00796b', labelKey: 'SETTINGS.COLOR_SARCELLE' },
    { id: 'rose', hex: '#c2185b', labelKey: 'SETTINGS.COLOR_ROSE' }
  ];
  private subs: Subscription[] = [];

  constructor(private themeService: ThemeService) {}

  ngOnInit(): void {
    this.subs.push(
      this.themeService.accent1$.subscribe(v => { this.accent1 = v; }),
      this.themeService.accent2$.subscribe(v => { this.accent2 = v; })
    );
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }

  selectPrimary(hex: string): void {
    this.themeService.setAccents(hex, this.accent2);
  }

  selectSecondary(hex: string): void {
    this.themeService.setAccents(this.accent1, hex);
  }

  /** Couleur personnalisée saisie (code hex) : appliquée + persistée comme les pastilles. */
  applyCustomPrimary(hex: string): void {
    const normalized = (hex || '').trim().toLowerCase();
    if (!/^#[0-9a-f]{6}$/.test(normalized)) return;
    this.customPrimary = normalized;
    this.themeService.setAccents(normalized, this.accent2);
  }

  applyCustomSecondary(hex: string): void {
    const normalized = (hex || '').trim().toLowerCase();
    if (!/^#[0-9a-f]{6}$/.test(normalized)) return;
    this.customSecondary = normalized;
    this.themeService.setAccents(this.accent1, normalized);
  }

  reset(): void {
    this.themeService.resetAccents();
  }
}
