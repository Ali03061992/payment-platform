import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { LoginService } from '../../services/login.service';
import { Order } from '../../models/order.model';

@Component({
  selector: 'app-document-order',
  templateUrl: './order-document.component.html',
  styleUrls: ['./order-document.component.css'],
  standalone: false
})
export class OrderDocumentComponent implements OnInit {
  order: Order | null = null;
  loading = true;
  error = '';

  constructor(private route: ActivatedRoute, private router: Router,
              private orders: OrderService, private login: LoginService) {}

  ngOnInit(): void {
    const reference = this.route.snapshot.paramMap.get('reference') || '';
    this.orders.getByReference(reference).subscribe({
      next: (order) => {
        this.order = order;
        this.loading = false;
      },
      error: () => {
        this.error = 'DOCUMENTS.NOT_FOUND';
        this.loading = false;
      }
    });
  }

  openFullDetail(): void {
    if (!this.order) return;
    if (this.login.hasRole('SUPPLIER_ADMIN', 'SUPPLIER_AGENT')) {
      this.router.navigate(['/dashboard/supplier/orders'], {
        queryParams: { ref: this.order.reference }
      });
    } else if (this.login.hasRole('SHOP_ADMIN', 'SHOP_AGENT', 'SHOP_MANAGER')) {
      this.router.navigate(['/dashboard/shop/orders', this.order.id]);
    }
  }

  downloadPurchaseOrder(): void {
    if (this.order) this.orders.downloadPurchaseOrder(this.order.id);
  }
}
