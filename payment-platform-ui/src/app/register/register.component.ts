import { Component } from '@angular/core';
import { Router } from '@angular/router';
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
    { value: 'SUPPLIER_ADMIN', label: 'Administrateur fournisseur' },
    { value: 'SUPPLIER_AGENT', label: 'Agent fournisseur' },
    { value: 'SHOP_ADMIN', label: 'Administrateur boutique' },
    { value: 'SHOP_AGENT', label: 'Agent boutique' }
  ];

  error = '';
  success = false;
  loading = false;

  constructor(private auth: AuthService, private router: Router) {}

  onSubmit(): void {
    this.error = '';
    this.loading = true;
    this.auth.register(this.form).subscribe({
      next: () => { this.success = true; this.loading = false; },
      error: (err) => { this.error = err.error?.message || "Erreur lors de l'inscription"; this.loading = false; }
    });
  }
}
