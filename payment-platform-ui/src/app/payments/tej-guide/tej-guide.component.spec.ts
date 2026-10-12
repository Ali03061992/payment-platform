import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TejGuideComponent } from './tej-guide.component';
import { TranslateStubPipe, translateServiceProvider } from '../../testing/translate-stubs';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('TejGuideComponent', () => {
  let component: TejGuideComponent;
  let fixture: ComponentFixture<TejGuideComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [TejGuideComponent, TranslateStubPipe],
      imports: [],
      providers: [
        translateServiceProvider(),
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    fixture = TestBed.createComponent(TejGuideComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should expose 7 steps', () => {
    expect(component.steps).toEqual([1, 2, 3, 4, 5, 6, 7]);
  });
});
