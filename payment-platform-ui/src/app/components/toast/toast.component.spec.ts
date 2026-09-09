import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ToastComponent } from './toast.component';
import { ToastService, Toast } from '../../services/toast.service';
import { of, Subject, Subscription } from 'rxjs';

describe('ToastComponent', () => {
  let component: ToastComponent;
  let fixture: ComponentFixture<ToastComponent>;
  let toastService: jasmine.SpyObj<ToastService>;
  let toastsSubject: Subject<Toast[]>;

  beforeEach(() => {
    toastsSubject = new Subject<Toast[]>();
    const toastSpy = jasmine.createSpyObj('ToastService', ['dismiss'], {
      toasts$: toastsSubject.asObservable()
    });

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [ToastComponent],
      providers: [
        { provide: ToastService, useValue: toastSpy }
      ]
    });
    fixture = TestBed.createComponent(ToastComponent);
    component = fixture.componentInstance;
    toastService = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should subscribe to toastService.toasts$', () => {
      component.ngOnInit();
      toastsSubject.next([{ id: 1, type: 'success', message: 'Test' }]);
      expect(component.toasts.length).toBe(1);
      expect(component.toasts[0].message).toBe('Test');
    });

    it('should update toasts on each emission', () => {
      component.ngOnInit();
      toastsSubject.next([{ id: 1, type: 'success', message: 'First' }]);
      expect(component.toasts.length).toBe(1);
      toastsSubject.next([{ id: 1, type: 'success', message: 'First' }, { id: 2, type: 'error', message: 'Second' }]);
      expect(component.toasts.length).toBe(2);
    });

    it('should handle empty toast list', () => {
      component.ngOnInit();
      toastsSubject.next([]);
      expect(component.toasts.length).toBe(0);
    });
  });

  describe('ngOnDestroy', () => {
    it('should unsubscribe from toasts$', () => {
      component.ngOnInit();
      const sub = component['sub'];
      spyOn(sub, 'unsubscribe');
      component.ngOnDestroy();
      expect(sub.unsubscribe).toHaveBeenCalled();
    });

    it('should handle null subscription gracefully', () => {
      component['sub'] = undefined as any;
      expect(() => component.ngOnDestroy()).not.toThrow();
    });
  });

  describe('getIcon', () => {
    it('should return correct icons', () => {
      expect(component.getIcon('success')).toBe('✓');
      expect(component.getIcon('error')).toBe('✕');
      expect(component.getIcon('warning')).toBe('⚠');
      expect(component.getIcon('info')).toBe('ℹ');
    });

    it('should return default icon for unknown type', () => {
      expect(component.getIcon('unknown' as any)).toBe('ℹ');
    });
  });

  describe('dismiss', () => {
    it('should call toastService.dismiss', () => {
      component.dismiss(1);
      expect(toastService.dismiss).toHaveBeenCalledWith(1);
    });

    it('should call toastService.dismiss with different id', () => {
      component.dismiss(42);
      expect(toastService.dismiss).toHaveBeenCalledWith(42);
    });
  });
});
