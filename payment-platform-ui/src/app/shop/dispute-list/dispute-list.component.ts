import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { DisputeService } from '../../services/dispute.service';
import { LoginService } from '../../services/login.service';
import { ToastService } from '../../services/toast.service';
import { Dispute } from '../../models/dispute.model';

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
  /** mine = périmètre de ma boutique ; all = toutes (admin). */
  scope: 'mine' | 'all' = 'mine';
  title = 'Mes réclamations';
  private subscriptions = new Subscription();

  constructor(
    private disputeService: DisputeService,
    private loginService: LoginService,
    private toast: ToastService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    if (this.route.snapshot.data['scope'] === 'all') {
      this.scope = 'all';
      this.title = 'Réclamations';
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
      error: (err: any) => {
        this.toast.error(err.error?.message || 'Erreur de chargement des réclamations');
        this.loading = false;
      }
    }));
  }

  get filtered(): Dispute[] {
    if (!this.filterStatus) return this.disputes;
    return this.disputes.filter((d) => d.status === this.filterStatus);
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
    const map: Record<string, string> = {
      OPEN: 'Ouvert', IN_PROGRESS: 'En cours', RESOLVED: 'Résolu', CLOSED: 'Fermé'
    };
    return map[s] || s;
  }

  statusClass(s: string): string {
    const map: Record<string, string> = {
      OPEN: 'pending', IN_PROGRESS: 'confirmed', RESOLVED: 'confirmed', CLOSED: 'cancelled'
    };
    return map[s] || '';
  }
}
