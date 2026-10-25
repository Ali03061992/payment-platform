import { Component } from '@angular/core';
import { BRAND } from '../core/brand.config';

@Component({
  selector: 'app-landing',
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.css'],
  standalone: false
})
export class LandingComponent {
  readonly brand = BRAND;
  readonly year = new Date().getFullYear();
  readonly phoneDisplay = '+216 24 176 545';
  readonly phoneLink = 'tel:+21624176545';
  readonly contactEmailLink =
    'mailto:ali.ben.amor.1992@hotmail.com?subject=' +
    encodeURIComponent("Demande d'information - NEXTERA");
  readonly whatsappLink =
    'https://wa.me/21624176545?text=' +
    encodeURIComponent('Bonjour, je souhaite des informations sur NEXTERA.');
}
