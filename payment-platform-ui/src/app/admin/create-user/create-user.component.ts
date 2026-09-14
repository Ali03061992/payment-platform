import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { UserService } from '../../services/user.service';
import { OrganizationService } from '../../services/organization.service';
import { Organization } from '../../models/organization.model';
import { ToastService } from '../../services/toast.service';

@Component({
    selector: 'app-create-user',
    templateUrl: './create-user.component.html',
    styleUrls: ['./create-user.component.css'],
    standalone: false
})
export class CreateUserComponent implements OnInit {
  form = {
    username: '',
    email: '',
    firstName: '',
    lastName: '',
    phone: '',
    role: 'SHOP_AGENT',
    organizationId: null as number | null
  };

  roles = [
    { value: 'SYSTEM_ADMIN', label: 'Administrateur système' },
    { value: 'SUPPLIER_ADMIN', label: 'Administrateur fournisseur' },
    { value: 'SUPPLIER_AGENT', label: 'Agent fournisseur' },
    { value: 'SHOP_ADMIN', label: 'Administrateur boutique' },
    { value: 'SHOP_AGENT', label: 'Agent boutique' }
  ];

  suppliers: Organization[] = [];
  shops: Organization[] = [];

  success = false;
  loading = false;

  constructor(
    private userService: UserService,
    private organizationService: OrganizationService,
    private router: Router,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.organizationService.listSuppliers().subscribe({
      next: (data) => this.suppliers = data,
      error: () => {}
    });
    this.organizationService.listShops().subscribe({
      next: (data) => this.shops = data,
      error: () => {}
    });
  }

  get requiresOrganization(): boolean {
    return this.form.role !== 'SYSTEM_ADMIN';
  }

  get organizationLabel(): string {
    const isSupplier = this.form.role.startsWith('SUPPLIER');
    return isSupplier ? 'Fournisseur' : 'Boutique';
  }

  get availableOrgs(): Organization[] {
    const isSupplier = this.form.role.startsWith('SUPPLIER');
    return isSupplier ? this.suppliers : this.shops;
  }

  onSubmit(): void {
    this.loading = true;
    const payload: any = {
      username: this.form.username,
      email: this.form.email,
      firstName: this.form.firstName,
      lastName: this.form.lastName,
      phone: this.form.phone,
      role: this.form.role
    };
    if (this.requiresOrganization && this.form.organizationId) {
      payload.organizationId = this.form.organizationId;
    }
    this.userService.create(payload).subscribe({
      next: () => { this.success = true; this.loading = false; this.toast.success('Compte créé avec succès'); },
      error: (err) => { this.toast.error(err.error?.message || "Erreur lors de la création"); this.loading = false; }
    });
  }

  goBack(): void {
    this.router.navigate(['/dashboard/admin/users']);
  }
}
