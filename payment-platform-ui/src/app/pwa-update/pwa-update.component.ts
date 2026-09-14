import { Component, OnInit } from '@angular/core';
import { SwUpdate, VersionReadyEvent } from '@angular/service-worker';
import { filter } from 'rxjs/operators';

@Component({
    selector: 'app-pwa-update',
    template: `
    @if (showUpdate) {
      <div class="pwa-update-banner">
        <span>Nouvelle version disponible !</span>
        <button (click)="updateApp()" class="btn-update">Mettre à jour</button>
        <button (click)="dismiss()" class="btn-dismiss">&times;</button>
      </div>
    }
    `,
    styles: [`
    .pwa-update-banner {
      position: fixed;
      bottom: 0;
      left: 0;
      right: 0;
      background: #1976d2;
      color: white;
      padding: 12px 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      z-index: 9999;
      box-shadow: 0 -2px 8px rgba(0,0,0,0.2);
    }
    .btn-update {
      background: white;
      color: #1976d2;
      border: none;
      padding: 6px 16px;
      border-radius: 4px;
      font-weight: bold;
      cursor: pointer;
    }
    .btn-dismiss {
      background: none;
      border: none;
      color: white;
      font-size: 20px;
      cursor: pointer;
      padding: 0 4px;
    }
  `],
    standalone: false
})
export class PwaUpdateComponent implements OnInit {
  showUpdate = false;

  constructor(private swUpdate: SwUpdate) {}

  ngOnInit() {
    if (this.swUpdate.isEnabled) {
      this.swUpdate.versionUpdates
        .pipe(filter((evt: any): evt is VersionReadyEvent => evt.type === 'VERSION_READY'))
        .subscribe(() => {
          this.showUpdate = true;
        });
    }
  }

  updateApp() {
    this.swUpdate.activateUpdate().then(() => {
      window.location.reload();
    });
  }

  dismiss() {
    this.showUpdate = false;
  }
}
