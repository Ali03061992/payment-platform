import { Component, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { UserService } from '../../services/user.service';
import { User } from '../../models/user.model';
import { ToastService } from '../../services/toast.service';

@Component({
    selector: 'app-account-activation',
    templateUrl: './account-activation.component.html',
    styleUrls: ['./account-activation.component.css'],
    standalone: false
})
export class AccountActivationComponent implements OnInit {
  users: User[] = [];
  loading = true;
  searchQuery = '';
  filterStatus = '';

  constructor(private userService: UserService, private toast: ToastService, private translate: TranslateService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.userService.list().subscribe({
      next: (users) => { this.users = users; this.loading = false; },
      error: (err) => { this.toast.error(err.error?.message || this.translate.instant('USERS.LOAD_ERROR')); this.loading = false; }
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
        this.toast.success(this.translate.instant('USERS.ACTIVATED_SUCCESS', { username: user.username }));
      },
      error: (err) => { this.toast.error(err.error?.message || this.translate.instant('COMMON.ERROR')); }
    });
  }

  disable(user: User): void {
    this.userService.disable(user.id).subscribe({
      next: () => {
        user.status = 'DISABLED';
        this.toast.success(this.translate.instant('USERS.DEACTIVATED_SUCCESS', { username: user.username }));
      },
      error: (err) => { this.toast.error(err.error?.message || this.translate.instant('COMMON.ERROR')); }
    });
  }
}
