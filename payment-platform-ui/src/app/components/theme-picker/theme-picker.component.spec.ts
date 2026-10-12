import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { BehaviorSubject } from 'rxjs';
import { ThemePickerComponent } from './theme-picker.component';
import { ThemeService } from '../../services/theme.service';
import { TranslateStubPipe, translateServiceProvider } from '../../testing/translate-stubs';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('ThemePickerComponent', () => {
  let component: ThemePickerComponent;
  let fixture: ComponentFixture<ThemePickerComponent>;
  let themeService: jasmine.SpyObj<ThemeService>;

  beforeEach(() => {
    const themeSpy = jasmine.createSpyObj('ThemeService', ['setAccents', 'resetAccents']);
    (themeSpy as any).accent1$ = new BehaviorSubject('#0284c7');
    (themeSpy as any).accent2$ = new BehaviorSubject('#e63946');

    TestBed.configureTestingModule({
      declarations: [ThemePickerComponent, TranslateStubPipe],
      imports: [FormsModule],
      providers: [
        translateServiceProvider(),
        { provide: ThemeService, useValue: themeSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    fixture = TestBed.createComponent(ThemePickerComponent);
    component = fixture.componentInstance;
    themeService = TestBed.inject(ThemeService) as jasmine.SpyObj<ThemeService>;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should apply custom primary color', () => {
    component.applyCustomPrimary('#123abc');
    expect(themeService.setAccents).toHaveBeenCalledWith('#123abc', '#e63946');
    expect(component.customPrimary).toBe('#123abc');
  });

  it('should reject invalid custom color', () => {
    component.applyCustomPrimary('not-a-color');
    expect(themeService.setAccents).not.toHaveBeenCalled();
  });

  it('should apply custom secondary color', () => {
    component.applyCustomSecondary('#ABCDEF');
    expect(themeService.setAccents).toHaveBeenCalledWith('#0284c7', '#abcdef');
  });
});
