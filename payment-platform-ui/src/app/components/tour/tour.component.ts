import { Component, OnInit, HostListener, ElementRef } from '@angular/core';
import { OnboardingService, TourStep } from '../../services/onboarding.service';

@Component({
  selector: 'app-tour',
  templateUrl: './tour.component.html',
  styleUrls: ['./tour.component.css'],
  standalone: false
})
export class TourComponent implements OnInit {
  isActive = false;
  currentStep = 0;
  steps: TourStep[] = [];
  tooltipPosition = { top: 0, left: 0 };
  highlightPosition = { top: 0, left: 0, width: 0, height: 0 };
  showHighlight = false;

  constructor(
    private onboardingService: OnboardingService,
    private el: ElementRef
  ) {}

  ngOnInit(): void {
    if (!this.onboardingService.isOnboardingCompleted()) {
      this.steps = this.onboardingService.getTourSteps();
      if (this.steps.length > 0) {
        setTimeout(() => this.startTour(), 800);
      }
    }
  }

  startTour(): void {
    this.isActive = true;
    this.currentStep = 0;
    this.positionTooltip();
  }

  nextStep(): void {
    if (this.currentStep < this.steps.length - 1) {
      this.currentStep++;
      this.positionTooltip();
    } else {
      this.completeTour();
    }
  }

  prevStep(): void {
    if (this.currentStep > 0) {
      this.currentStep--;
      this.positionTooltip();
    }
  }

  skipTour(): void {
    this.completeTour();
  }

  completeTour(): void {
    this.isActive = false;
    this.showHighlight = false;
    this.onboardingService.completeOnboarding();
  }

  private positionTooltip(): void {
    const step = this.steps[this.currentStep];
    if (step?.targetSelector) {
      const target = document.querySelector(step.targetSelector) as HTMLElement;
      if (target) {
        target.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        const rect = target.getBoundingClientRect();
        this.showHighlight = true;
        this.highlightPosition = {
          top: rect.top,
          left: rect.left,
          width: rect.width,
          height: rect.height,
        };
        const TOOLTIP_W = Math.min(360, window.innerWidth - 48);
        const TOOLTIP_H = 340;
        let top = rect.bottom + 12;
        if (top + TOOLTIP_H > window.innerHeight - 16) {
          top = rect.top - TOOLTIP_H - 12;
        }
        if (top < 16) top = 16;
        let left = rect.left + rect.width / 2;
        if (left - TOOLTIP_W / 2 < 16) left = TOOLTIP_W / 2 + 16;
        if (left + TOOLTIP_W / 2 > window.innerWidth - 16) left = window.innerWidth - TOOLTIP_W / 2 - 16;
        this.tooltipPosition = { top, left };
        return;
      }
    }
    this.showHighlight = false;
    this.tooltipPosition = { top: window.innerHeight / 2, left: window.innerWidth / 2 };
  }

  @HostListener('window:resize')
  onResize(): void {
    if (this.isActive) {
      this.positionTooltip();
    }
  }

  get progressPercent(): number {
    return ((this.currentStep + 1) / this.steps.length) * 100;
  }

  get isLastStep(): boolean {
    return this.currentStep === this.steps.length - 1;
  }

  get isFirstStep(): boolean {
    return this.currentStep === 0;
  }
}
