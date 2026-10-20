import { Component } from '@angular/core';
import { ThemeService } from './services/theme.service';
import { BRAND } from './core/brand.config';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css'],
    standalone: false
})
export class AppComponent {
  title = BRAND.name;

  constructor(private themeService: ThemeService) {}
}
