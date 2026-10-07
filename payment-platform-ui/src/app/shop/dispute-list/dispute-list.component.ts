import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { DisputeService } from '../../services/dispute.service';
import { LoginService } from '../../services/login.service';
import { ToastService } from '../../services/toast.service';
import { Dispute } from '../../models/dispute.model';
import { statusLabelFr } from '../../pipes/status-label.pipe';
import { sortItems, toggleSortState, ariaSortFor, sortIndicatorFor, SortState } from '../../models/page.model';

@Component({
    selector: 'app-dispute-list',
    templateUrl: './dispute-list.component.html',
    styleUrls: ['./dispute-list.component.css'],
    standalone: false
})
export class DisputeListComponent implements OnInit, OnDestroy {
  disputes: Dispute[] = [];
  loading = true;
  filterStatus = '';
  scope: 'mine' | 'all' = 'mine';
  title = 'DISPUTES.MY_TITLE';
  sort: SortState = { field: null, direction: 'asc' };
  private subscriptions = new Subscription();

  constructor(
    private disputeService: DisputeService,
    private loginService: LoginService,
    private toast: ToastService,
    private router: Router,
    private route: ActivatedRoute,
    private translate: TranslateService
  ) {}

  ngOnInit(): void {
    if (this.route.snapshot.data['scope'] === 'all') {
      this.scope = 'all';
      this.title = 'DISPUTES.ALL_TITLE';
    }
    this.load();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  load(): void {
    this.loading = true;
    this.subscriptions.add(this.disputeService.list().subscribe({
      next: (all) => {
        const orgId = this.loginService.getCurrentUser()?.organizationId;
        this.disputes = (all || []).filter((d) => this.scope === 'all' || !orgId || d.shopId === orgId);
        this.loading = false;
      },
      error: (err: { error?: { message?: string }; status?: number; statusText?: string; message?: string }) => {
        this.toast.error(err.error?.message || this.translate.instant('DISPUTES.LOAD_ERROR'));
        this.loading = false;
      }
    }));
  }

  get filtered(): Dispute[] {
    const base = !this.filterStatus ? this.disputes : this.disputes.filter((d) => d.status === this.filterStatus);
    return sortItems(base, this.sort.field, this.sort.direction);
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

  openDispute(id: string): void {
    const roles = this.loginService.getCurrentUser()?.roles || [];
    if (roles.includes('SYSTEM_ADMIN')) {
      this.router.navigate(['/dashboard/admin/disputes', id]);
    } else {
      this.router.navigate(['/dashboard/shop/disputes', id]);
    }
  }

  statusLabel(s: string): string {
    return statusLabelFr(s);
  }

  statusClass(s: string): string {
    const map: Record<string, string> = {
      OPEN: 'pending', IN_PROGRESS: 'confirmed', RESOLVED: 'confirmed', CLOSED: 'cancelled'
    };
    return map[s] || '';
  }
}