import { Component, OnInit } from '@angular/core';
import { OrganizationService } from '../../services/organization.service';
import { OrganizationStats } from '../../models/organization.model';

@Component({
  selector: 'app-organization-stats',
  templateUrl: './organization-stats.component.html',
  styleUrls: ['./organization-stats.component.css']
})
export class OrganizationStatsComponent implements OnInit {
  stats: OrganizationStats | null = null;
  loading = true;

  constructor(private orgService: OrganizationService) {}

  ngOnInit(): void {
    this.orgService.getStats().subscribe({
      next: (data: OrganizationStats) => { this.stats = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }
}
