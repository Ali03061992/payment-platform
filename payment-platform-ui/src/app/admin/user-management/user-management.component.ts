import { Component, OnInit } from '@angular/core';
import { UserService } from '../../services/user.service';
import { User } from '../../models/user.model';
import { ToastService } from '../../services/toast.service';

@Component({
    selector: 'app-user-management',
    templateUrl: './user-management.component.html',
    styleUrls: ['./user-management.component.css'],
    standalone: false
})
export class UserManagementComponent implements OnInit {
  users: User[] = [];
  loading = true;
  error = '';
  filterRole = '';
  filterStatus = '';

  constructor(private userService: UserService, private toast: ToastService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.userService.list(undefined, this.filterRole || undefined, this.filterStatus || undefined).subscribe({
      next: (users) => { this.users = users; this.loading = false; },
      error: (err) => { this.error = err.error?.message || 'Erreur de chargement'; this.loading = false; }
    });
  }

  activate(user: User): void {
    this.userService.activate(user.id).subscribe({
      next: () => {
        user.status = 'ACTIVE';
        this.toast.success(`${user.username} activé avec succès`);
      },
      error: (err) => { this.toast.error(err.error?.message || 'Erreur'); }
    });
  }

  disable(user: User): void {
    this.userService.disable(user.id).subscribe({
      next: () => {
        user.status = 'DISABLED';
        this.toast.success(`${user.username} désactivé avec succès`);
      },
      error: (err) => { this.toast.error(err.error?.message || 'Erreur'); }
    });
  }

  applyFilter(): void {
    this.loadUsers();
  }
}
