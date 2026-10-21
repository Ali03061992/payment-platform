import { Component, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { OrganizationService } from '../../services/organization.service';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-supplier-fiscal-settings',
  templateUrl: './fiscal-settings.component.html',
  styleUrls: ['./fiscal-settings.component.css'],
  standalone: false
})
export class SupplierFiscalSettingsComponent implements OnInit {
  loading = true;
  saving = false;
  teifEnabled = true;
  tejEnabled = true;
  supplierName = '';

  constructor(private organizations: OrganizationService, private toast: ToastService,
              private translate: TranslateService) {}

  ngOnInit(): void {
    this.organizations.getFiscalSettings().subscribe({
      next: (org) => {
        this.supplierName = org.name;
        this.teifEnabled = org.teifEnabled !== false;
        this.tejEnabled = org.tejEnabled !== false;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toast.error(this.translate.instant('FISCAL_SETTINGS.LOAD_ERROR'));
      }
    });
  }

  save(): void {
    this.saving = true;
    this.organizations.updateFiscalSettings({
      teifEnabled: this.teifEnabled,
      tejEnabled: this.tejEnabled
    }).subscribe({
      next: (org) => {
        this.teifEnabled = org.teifEnabled !== false;
        this.tejEnabled = org.tejEnabled !== false;
        this.saving = false;
        this.toast.success(this.translate.instant('FISCAL_SETTINGS.SAVED'));
      },
      error: () => {
        this.saving = false;
        this.toast.error(this.translate.instant('FISCAL_SETTINGS.SAVE_ERROR'));
      }
    });
  }
}
