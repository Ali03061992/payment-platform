import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { PaginationComponent } from './pagination.component';

describe('PaginationComponent', () => {
  let component: PaginationComponent;
  let fixture: ComponentFixture<PaginationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [PaginationComponent],
      imports: [FormsModule]
    }).compileComponents();
    fixture = TestBed.createComponent(PaginationComponent);
    component = fixture.componentInstance;
  });

  it('devrait créer le composant', () => {
    expect(component).toBeTruthy();
  });

  it('devrait calculer les pages et la fenêtre', () => {
    component.totalElements = 95;
    component.size = 10;
    component.page = 0;
    expect(component.totalPages).toBe(10);
    expect(component.pageNumbers).toEqual([0, 1, 2, 3, 4]);
    expect(component.from).toBe(1);
    expect(component.to).toBe(10);
  });

  it('devrait émettre pageChange et sizeChange', () => {
    component.totalElements = 50;
    component.size = 10;
    component.page = 1;
    let emittedPage = -1;
    let emittedSize = -1;
    component.pageChange.subscribe(p => emittedPage = p);
    component.sizeChange.subscribe(s => emittedSize = s);
    component.next();
    component.onSizeChange('20');
    expect(emittedPage).toBe(2);
    expect(emittedSize).toBe(20);
  });
});
