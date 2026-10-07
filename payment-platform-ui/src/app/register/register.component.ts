import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { AuthService } from '../services/auth.service';

@Component({
    selector: 'app-register',
    templateUrl: './register.component.html',
    styleUrls: ['./register.component.css'],
    standalone: false
})
export class RegisterComponent {
  form = {
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    phone: '',
    role: 'SHOP_AGENT'
  };

  roles = [
    { value: 'SUPPLIER_ADMIN', label: 'AUTH.ROLE_SUPPLIER_ADMIN' },
    { value: 'SUPPLIER_AGENT', label: 'AUTH.ROLE_SUPPLIER_AGENT' },
    { value: 'SHOP_ADMIN', label: 'AUTH.ROLE_SHOP_ADMIN' },
    { value: 'SHOP_AGENT', label: 'AUTH.ROLE_SHOP_AGENT' }
  ];

  error = '';
  success = false;
  loading = false;

  constructor(private auth: AuthService, private router: Router, private translate: TranslateService) {}

  onSubmit(): void {
    this.error = '';
    this.loading = true;
    this.auth.register(this.form).subscribe({
      next: () => { this.success = true; this.loading = false; },
      error: (err) => { this.error = err.error?.message || this.translate.instant('AUTH.REGISTER_ERROR'); this.loading = false; }
    });
  }
}
