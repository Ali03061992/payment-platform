import { Component, OnInit } from '@angular/core';
import { NotificationService } from '../../services/notification.service';
import { PushNotificationService } from '../../services/push-notification.service';

@Component({
    selector: 'app-notification-banner',
    templateUrl: './notification-banner.component.html',
    styleUrls: ['./notification-banner.component.css'],
    standalone: false
})
export class NotificationBannerComponent implements OnInit {
  showBanner = false;
  permissionStatus: NotificationPermission | 'unsupported' = 'default';

  constructor(
    private notificationService: NotificationService,
    private pushNotificationService: PushNotificationService
  ) {}

  ngOnInit(): void {
    this.checkPermission();
  }

  private checkPermission(): void {
    this.permissionStatus = this.notificationService.getPermissionStatus();
    const userChoice = localStorage.getItem('notification_choice');

    if (this.permissionStatus === 'unsupported') {
      this.showBanner = false;
      return;
    }

    if (!userChoice && this.permissionStatus !== 'granted' && this.permissionStatus !== 'denied') {
      this.showBanner = true;
    }
  }

  async acceptNotifications(): Promise<void> {
    const result = await this.notificationService.requestPermission();
    this.permissionStatus = result;
    localStorage.setItem('notification_choice', 'accepted');
    this.showBanner = false;

    if (result === 'granted') {
      this.notificationService.showBrowserNotification();
      try {
        await this.pushNotificationService.requestPermissionAndGetToken();
        this.pushNotificationService.listenToMessages();
      } catch {
        // Push optionnel : ne jamais bloquer l'acceptation
      }
    }
  }

  dismissNotifications(): void {
    localStorage.setItem('notification_choice', 'dismissed');
    this.showBanner = false;
  }
}
