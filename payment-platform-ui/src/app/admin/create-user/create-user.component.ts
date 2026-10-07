import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
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
    { value: 'SYSTEM_ADMIN', label: 'AUTH.ROLE_SYSTEM_ADMIN' },
    { value: 'SUPPLIER_ADMIN', label: 'AUTH.ROLE_SUPPLIER_ADMIN' },
    { value: 'SUPPLIER_AGENT', label: 'AUTH.ROLE_SUPPLIER_AGENT' },
    { value: 'SHOP_ADMIN', label: 'AUTH.ROLE_SHOP_ADMIN' },
    { value: 'SHOP_AGENT', label: 'AUTH.ROLE_SHOP_AGENT' }
  ];

  suppliers: Organization[] = [];
  shops: Organization[] = [];

  success = false;
  loading = false;

  constructor(
    private userService: UserService,
    private organizationService: OrganizationService,
    private router: Router,
    private toast: ToastService,
    private translate: TranslateService
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
    return this.translate.instant(isSupplier ? 'ORGS.SUPPLIER' : 'ORGS.SHOP');
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
      next: () => { this.success = true; this.loading = false; this.toast.success(this.translate.instant('USERS.CREATED_TOAST')); },
      error: (err) => { this.toast.error(err.error?.message || this.translate.instant('USERS.CREATE_ERROR')); this.loading = false; }
    });
  }

  goBack(): void {
    this.router.navigate(['/dashboard/admin/users']);
  }
}
