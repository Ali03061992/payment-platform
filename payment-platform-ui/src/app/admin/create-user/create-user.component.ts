import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-create-user',
  templateUrl: './create-user.component.html',
  styleUrls: ['./create-user.component.css']
})
export class CreateUserComponent {
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
    { value: 'SYSTEM_ADMIN', label: 'Administrateur système' },
    { value: 'SUPPLIER_ADMIN', label: 'Administrateur fournisseur' },
    { value: 'SUPPLIER_AGENT', label: 'Agent fournisseur' },
    { value: 'SHOP_ADMIN', label: 'Administrateur boutique' },
    { value: 'SHOP_AGENT', label: 'Agent boutique' },
    { value: 'SALES', label: 'Commercial (Sales)' }
  ];

  error = '';
  success = false;
  loading = false;

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit(): void {
    this.error = '';
    this.loading = true;
    this.authService.register(this.form).subscribe({
      next: () => { this.success = true; this.loading = false; },
      error: (err) => { this.error = err.error?.message || "Erreur lors de la création"; this.loading = false; }
    });
  }

  goBack(): void {
    this.router.navigate(['/admin/users']);
  }
}
