import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface Toast {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
  duration?: number;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private toastsSubject = new BehaviorSubject<Toast[]>([]);
  private counter = 0;

  toasts$: Observable<Toast[]> = this.toastsSubject.asObservable();

  show(message: string, type: Toast['type'] = 'info', duration = 4000): void {
    const toast: Toast = {
      id: String(++this.counter),
      type,
      message,
      duration
    };

    const current = this.toastsSubject.getValue();
    this.toastsSubject.next([...current, toast]);

    if (duration > 0) {
      setTimeout(() => this.dismiss(toast.id), duration);
    }
  }

  success(message: string, duration = 4000): void {
    this.show(message, 'success', duration);
  }

  error(message: string, duration = 6000): void {
    this.show(message, 'error', duration);
  }

  warning(message: string, duration = 5000): void {
    this.show(message, 'warning', duration);
  }

  info(message: string, duration = 4000): void {
    this.show(message, 'info', duration);
  }

  dismiss(id: string): void {
    const current = this.toastsSubject.getValue();
    this.toastsSubject.next(current.filter(t => t.id !== id));
  }

  clear(): void {
    this.toastsSubject.next([]);
  }
}
