// @ts-nocheck
/**
 * Tests du composant AppIconComponent.
 * Perimetre : rendu SVG, fallback nom inconnu, taille.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AppIconComponent } from './icon.component';

describe('AppIconComponent', () => {
  let component: AppIconComponent;
  let fixture: ComponentFixture<AppIconComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [AppIconComponent]
    });
    fixture = TestBed.createComponent(AppIconComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render an svg with the requested size', () => {
    component.name = 'users';
    component.size = 24;
    fixture.detectChanges();
    const svg = fixture.nativeElement.querySelector('svg');
    expect(svg).not.toBeNull();
    expect(svg.getAttribute('width')).toBe('24');
    expect(svg.getAttribute('height')).toBe('24');
    expect(svg.innerHTML).toContain('circle');
  });

  it('should fall back to dashboard for unknown names', () => {
    component.name = 'nope-unknown';
    fixture.detectChanges();
    const svg = fixture.nativeElement.querySelector('svg');
    expect(svg.innerHTML).toContain('rect');
  });

  it('should be aria-hidden (decorative)', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('svg').getAttribute('aria-hidden')).toBe('true');
  });
});
