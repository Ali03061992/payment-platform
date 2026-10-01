import { Component, EventEmitter, Input, Output } from '@angular/core';
import { pageRange, pageWindow } from '../../models/page.model';

/**
 * Pagination homogène (lazy loading) : Précédent/Suivant + numéros,
 * compteur "X-Y sur Z" et taille de page 10/20/50.
 * Les pages sont 0-based ; le parent recharge ses données via (pageChange)/(sizeChange).
 */
@Component({
  selector: 'app-pagination',
  templateUrl: './pagination.component.html',
  styleUrls: ['./pagination.component.css'],
  standalone: false
})
export class PaginationComponent {
  @Input() page = 0;
  @Input() size = 20;
  @Input() totalElements = 0;
  @Input() pageSizes: number[] = [10, 20, 50];
  @Input() showSize = true;

  @Output() pageChange = new EventEmitter<number>();
  @Output() sizeChange = new EventEmitter<number>();

  get totalPages(): number {
    if (!this.size || this.size <= 0) return 0;
    return Math.ceil(this.totalElements / this.size);
  }

  get from(): number {
    return pageRange(this.page, this.size, this.totalElements).from;
  }

  get to(): number {
    return pageRange(this.page, this.size, this.totalElements).to;
  }

  get pageNumbers(): number[] {
    return pageWindow(this.page, this.totalPages);
  }

  get canPrev(): boolean {
    return this.page > 0;
  }

  get canNext(): boolean {
    return this.page < this.totalPages - 1;
  }

  prev(): void {
    if (this.canPrev) this.pageChange.emit(this.page - 1);
  }

  next(): void {
    if (this.canNext) this.pageChange.emit(this.page + 1);
  }

  goTo(p: number): void {
    if (p >= 0 && p < this.totalPages && p !== this.page) this.pageChange.emit(p);
  }

  onSizeChange(value: string | number): void {
    const size = typeof value === 'string' ? parseInt(value, 10) : value;
    if (size > 0 && size !== this.size) this.sizeChange.emit(size);
  }
}
