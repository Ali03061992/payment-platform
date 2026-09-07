import { Pipe, PipeTransform } from '@angular/core';
import { ProductOptimization } from '../../models/stock-optimization.model';

@Pipe({ name: 'filterByRisk', pure: false })
export class FilterByRiskPipe implements PipeTransform {
  transform(products: ProductOptimization[]): ProductOptimization[] {
    if (!products) return [];
    return products.filter(p =>
      p.stockoutLevel === 'HIGH' || p.stockoutLevel === 'CRITICAL' ||
      p.overstockRisk > 0.5 || p.recommendation.action === 'ORDER_NOW'
    );
  }
}
