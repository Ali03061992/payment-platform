import { Component, OnInit } from '@angular/core';
import { UserService } from '../../services/user.service';
import { User } from '../../models/user.model';

@Component({
  selector: 'app-user-management',
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.css']
})
export class UserManagementComponent implements OnInit {
  users: User[] = [];
  loading = true;
  error = '';
  filterRole = '';
  filterStatus = '';
  successMessage = '';

  constructor(private userService: UserService) {}

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
        this.successMessage = `${user.username} activé avec succès`;
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => { this.error = err.error?.message || 'Erreur'; setTimeout(() => this.error = '', 3000); }
    });
  }

  disable(user: User): void {
    this.userService.disable(user.id).subscribe({
      next: () => {
        user.status = 'DISABLED';
        this.successMessage = `${user.username} désactivé avec succès`;
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => { this.error = err.error?.message || 'Erreur'; setTimeout(() => this.error = '', 3000); }
    });
  }

  applyFilter(): void {
    this.loadUsers();
  }
}
