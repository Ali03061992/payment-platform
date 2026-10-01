import { Component, OnInit } from '@angular/core';
import { UserService } from '../../services/user.service';
import { User, UserPage } from '../../models/user.model';
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
  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;

  constructor(private userService: UserService, private toast: ToastService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.userService.listPaged(
      undefined, this.filterRole || undefined, this.filterStatus || undefined,
      this.currentPage, this.pageSize
    ).subscribe({
      next: (page: UserPage) => {
        this.users = page.items || [];
        this.totalElements = page.totalElements ?? this.users.length;
        this.totalPages = page.totalPages ?? 1;
        this.currentPage = page.number ?? this.currentPage;
        this.loading = false;
      },
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
    this.currentPage = 0;
    this.loadUsers();
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadUsers();
  }

  onSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.loadUsers();
  }
}
