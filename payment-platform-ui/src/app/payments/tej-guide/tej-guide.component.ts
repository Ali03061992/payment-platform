import { Component } from '@angular/core';

/**
 * Page d'aide : utiliser la plateforme TEJ pour les certificats de retenue.
 * Contenu statique trilingue (clés TEJ_GUIDE.*), sans appel API.
 */
@Component({
    selector: 'app-tej-guide',
    templateUrl: './tej-guide.component.html',
    styleUrls: ['./tej-guide.component.css'],
    standalone: false
})
export class TejGuideComponent {
  steps = [1, 2, 3, 4, 5, 6, 7];
}
