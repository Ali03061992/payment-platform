// @ts-nocheck
/**
 * Tests du composant ConfirmDialogComponent.
 * Perimetre : affichage, boutons Valider/Non, fermeture fond et Echap.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConfirmDialogComponent } from './confirm-dialog.component';
import { ConfirmDialogService } from './confirm-dialog.service';
import { AppIconComponent } from '../icon/icon.component';

describe('ConfirmDialogComponent', () => {
  let component: ConfirmDialogComponent;
  let fixture: ComponentFixture<ConfirmDialogComponent>;
  let dialogs: ConfirmDialogService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ConfirmDialogComponent, AppIconComponent]
    });
    fixture = TestBed.createComponent(ConfirmDialogComponent);
    component = fixture.componentInstance;
    dialogs = TestBed.inject(ConfirmDialogService);
    fixture.detectChanges();
  });

  it('should create hidden', () => {
    expect(component).toBeTruthy();
    expect(fixture.nativeElement.querySelector('[data-testid="confirm-dialog"]')).toBeNull();
  });

  it('should show dialog with Valider/Non buttons', () => {
    dialogs.confirm({ title: 'Rejeter ?', message: 'Confirmer le rejet' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="confirm-dialog"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="confirm-ok"]').textContent).toContain('Valider');
    expect(fixture.nativeElement.querySelector('[data-testid="confirm-cancel"]').textContent).toContain('Non');
  });

  it('should answer true on Valider and hide', () => {
    let received: boolean | null = null;
    dialogs.confirm({ title: 'T', message: 'M' }).subscribe(v => received = v);
    fixture.detectChanges();
    fixture.nativeElement.querySelector('[data-testid="confirm-ok"]').click();
    fixture.detectChanges();
    expect(received).toBeTrue();
    expect(fixture.nativeElement.querySelector('[data-testid="confirm-dialog"]')).toBeNull();
  });

  it('should answer false on Non', () => {
    let received: boolean | null = null;
    dialogs.confirm({ title: 'T', message: 'M' }).subscribe(v => received = v);
    fixture.detectChanges();
    fixture.nativeElement.querySelector('[data-testid="confirm-cancel"]').click();
    expect(received).toBeFalse();
  });

  it('should answer false on overlay click', () => {
    let received: boolean | null = null;
    dialogs.confirm({ title: 'T', message: 'M' }).subscribe(v => received = v);
    fixture.detectChanges();
    fixture.nativeElement.querySelector('[data-testid="confirm-overlay"]').click();
    expect(received).toBeFalse();
  });

  it('should answer false on escape', () => {
    let received: boolean | null = null;
    dialogs.confirm({ title: 'T', message: 'M' }).subscribe(v => received = v);
    fixture.detectChanges();
    component.onEscape();
    expect(received).toBeFalse();
  });
});
