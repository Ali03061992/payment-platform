// @ts-nocheck
import { TestBed } from '@angular/core/testing';
import { ToastService, Toast } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    jasmine.clock().install();
    TestBed.configureTestingModule({
      providers: [ToastService]
    });
    service = TestBed.inject(ToastService);
  });

  afterEach(() => {
    jasmine.clock().uninstall();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('show', () => {
    it('should add toast with default type info', () => {
      let toasts: Toast[] = [];
      service.toasts$.subscribe(t => toasts = t);
      service.show('Test message');
      expect(toasts.length).toBe(1);
      expect(toasts[0].message).toBe('Test message');
      expect(toasts[0].type).toBe('info');
    });

    it('should auto dismiss after duration', () => {
      let toasts: Toast[] = [];
      service.toasts$.subscribe(t => toasts = t);
      service.show('Test', 'success', 4000);
      expect(toasts.length).toBe(1);
      jasmine.clock().tick(4001);
      expect(toasts.length).toBe(0);
    });

    it('should not auto dismiss when duration is 0', () => {
      let toasts: Toast[] = [];
      service.toasts$.subscribe(t => toasts = t);
      service.show('Test', 'info', 0);
      expect(toasts.length).toBe(1);
      jasmine.clock().tick(10000);
      expect(toasts.length).toBe(1);
    });
  });

  describe('success', () => {
    it('should add success toast', () => {
      let toasts: Toast[] = [];
      service.toasts$.subscribe(t => toasts = t);
      service.success('Done!');
      expect(toasts[0].type).toBe('success');
      expect(toasts[0].message).toBe('Done!');
    });
  });

  describe('error', () => {
    it('should add error toast with longer duration', () => {
      let toasts: Toast[] = [];
      service.toasts$.subscribe(t => toasts = t);
      service.error('Error!');
      expect(toasts[0].type).toBe('error');
      expect(toasts[0].duration).toBe(6000);
    });
  });

  describe('warning', () => {
    it('should add warning toast', () => {
      let toasts: Toast[] = [];
      service.toasts$.subscribe(t => toasts = t);
      service.warning('Warning!');
      expect(toasts[0].type).toBe('warning');
      expect(toasts[0].duration).toBe(5000);
    });
  });

  describe('info', () => {
    it('should add info toast', () => {
      let toasts: Toast[] = [];
      service.toasts$.subscribe(t => toasts = t);
      service.info('Info!');
      expect(toasts[0].type).toBe('info');
    });
  });

  describe('dismiss', () => {
    it('should remove toast by id', () => {
      let toasts: Toast[] = [];
      service.toasts$.subscribe(t => toasts = t);
      service.show('Test1', 'info', 0);
      service.show('Test2', 'success', 0);
      expect(toasts.length).toBe(2);
      service.dismiss(toasts[0].id);
      expect(toasts.length).toBe(1);
      expect(toasts[0].message).toBe('Test2');
    });
  });

  describe('clear', () => {
    it('should remove all toasts', () => {
      let toasts: Toast[] = [];
      service.toasts$.subscribe(t => toasts = t);
      service.show('Test1', 'info', 0);
      service.show('Test2', 'success', 0);
      expect(toasts.length).toBe(2);
      service.clear();
      expect(toasts.length).toBe(0);
    });
  });

  describe('counter', () => {
    it('should increment id for each toast', () => {
      let toasts: Toast[] = [];
      service.toasts$.subscribe(t => toasts = t);
      service.show('First', 'info', 0);
      service.show('Second', 'info', 0);
      service.show('Third', 'info', 0);
      expect(toasts[0].id).toBe(1);
      expect(toasts[1].id).toBe(2);
      expect(toasts[2].id).toBe(3);
    });
  });
});
