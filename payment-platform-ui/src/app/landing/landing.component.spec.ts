import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { LandingComponent } from './landing.component';
import { TranslateStubPipe, translateServiceProvider } from '../testing/translate-stubs';

describe('LandingComponent', () => {
  let component: LandingComponent;
  let fixture: ComponentFixture<LandingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [LandingComponent, TranslateStubPipe],
      providers: [translateServiceProvider()],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(LandingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should expose the requested contact links', () => {
    expect(component.phoneLink).toBe('tel:+21624176545');
    expect(component.contactEmailLink).toContain('mailto:ali.ben.amor.1992@hotmail.com');
    expect(component.whatsappLink).toContain('https://wa.me/21624176545');
  });

  it('should not display the email address in the page', () => {
    expect(fixture.nativeElement.textContent).not.toContain('ali.ben.amor.1992@hotmail.com');
  });

  it('should not expose public self-registration', () => {
    const links: HTMLAnchorElement[] = Array.from(fixture.nativeElement.querySelectorAll('a'));
    expect(links.some(link => link.getAttribute('href') === '/register')).toBeFalse();
    expect(fixture.nativeElement.textContent).not.toContain('LANDING.REGISTER');
  });

  it('should expose contact and login entry points', () => {
    const links: HTMLAnchorElement[] = Array.from(fixture.nativeElement.querySelectorAll('a'));
    expect(links.some(link => link.getAttribute('href') === '#contact')).toBeTrue();
    expect(links.some(link =>
      link.getAttribute('href') === '/login' || link.getAttribute('routerLink') === '/login'
    )).toBeTrue();
  });
});
