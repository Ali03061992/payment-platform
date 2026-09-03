import { Component, OnInit } from '@angular/core';
import { UserService } from '../../services/user.service';
import { User } from '../../models/user.model';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-account-activation',
  templateUrl: './account-activation.component.html',
  styleUrls: ['./account-activation.component.css']
})
export class AccountActivationComponent implements OnInit {
  users: User[] = [];
  loading = true;
  searchQuery = '';
  filterStatus = '';

  constructor(private userService: UserService, private toast: ToastService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.userService.list().subscribe({
      next: (users) => { this.users = users; this.loading = false; },
      error: (err) => { this.toast.error(err.error?.message || 'Erreur de chargement'); this.loading = false; }
    });
  }

  get filteredUsers(): User[] {
    return this.users.filter(u => {
      const matchSearch = !this.searchQuery ||
        u.username.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        u.firstName.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        u.lastName.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        u.email.toLowerCase().includes(this.searchQuery.toLowerCase());
      const matchStatus = !this.filterStatus || u.status === this.filterStatus;
      return matchSearch && matchStatus;
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
}
