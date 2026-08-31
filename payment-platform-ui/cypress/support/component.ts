/// <reference types="cypress" />
/// <reference types="@cypress/angular" />

import '@cypress/angular/ct/component';

// Component testing configuration
declare global {
  interface Window {
    __env?: Record<string, string>;
  }
}

// Global styles for component testing
const style = document.createElement('style');
style.textContent = `
  /* Ensure components have proper sizing in CT */
  .cdk-overlay-container { z-index: 10000; }
  
  /* Mock material icons if needed */
  .mat-icon { font-family: 'Material Icons'; }
  
  /* Reset for consistent component rendering */
  * { box-sizing: border-box; }
  body { margin: 0; padding: 16px; font-family: 'Inter', sans-serif; }
`;
document.head.appendChild(style);

// Angular test bed setup
import { TestBed } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

// Re-export for component tests
export { TestBed, BrowserAnimationsModule, HttpClientTestingModule, HttpTestingController, RouterTestingModule };

// Auto-setup TestBed for each component test
beforeEach(() => {
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    imports: [
      BrowserAnimationsModule,
      HttpClientTestingModule,
      RouterTestingModule,
    ],
    // Common providers for most components
    providers: [
      // Add common providers here
    ],
  }).compileComponents();
});