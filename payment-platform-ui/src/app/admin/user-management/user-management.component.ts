import { Component, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { UserService } from '../../services/user.service';
import { User, UserPage } from '../../models/user.model';
import { ToastService } from '../../services/toast.service';
import { sortItems, toggleSortState, ariaSortFor, sortIndicatorFor, SortState } from '../../models/page.model';

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
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;
  sort: SortState = { field: null, direction: 'asc' };

  constructor(private userService: UserService, private toast: ToastService, private translate: TranslateService) {}

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
      error: (err) => { this.error = err.error?.message || this.translate.instant('USERS.LOAD_ERROR'); this.loading = false; }
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

  onSort(field: string): void {
    this.sort = toggleSortState(this.sort, field);
  }

  ariaSort(field: string): 'ascending' | 'descending' | 'none' {
    return ariaSortFor(field, this.sort);
  }

  sortIndicator(field: string): string {
    return sortIndicatorFor(field, this.sort);
  }

  get sortedUsers(): User[] {
    return sortItems(this.users, this.sort.field, this.sort.direction);
  }
}
