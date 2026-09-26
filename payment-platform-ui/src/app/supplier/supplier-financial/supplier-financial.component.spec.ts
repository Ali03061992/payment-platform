// @ts-nocheck
/**
 * Tests du composant SupplierFinancialComponent.
 * Perimetre : instanciation et comportements decrits dans les blocs it (voir blocs describe/it).
 * Moyens : TestBed.
 */
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { SupplierFinancialComponent } from './supplier-financial.component';

describe('SupplierFinancialComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [SupplierFinancialComponent],
      providers: [provideHttpClient()]
    }).compileComponents();
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(SupplierFinancialComponent);
    const component = fixture.componentInstance;
    expect(component).toBeDefined();
  });
});
