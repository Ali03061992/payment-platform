import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { LoginService } from '../services/login.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  form = { username: '', password: '' };
  error = '';
  loading = false;

  constructor(private loginService: LoginService, private router: Router) {}

  onSubmit(): void {
    this.error = '';
    this.loading = true;
    this.loginService.login(this.form).subscribe({
      next: (res) => {
        localStorage.setItem('token', res.accessToken);
        this.loginService.getMe().subscribe({
          next: (user) => {
            localStorage.setItem('user', JSON.stringify(user));
            this.loading = false;
            this.router.navigate(['/dashboard']);
          },
          error: () => {
            localStorage.setItem('user', JSON.stringify({
              username: this.form.username,
              roles: ['SYSTEM_ADMIN'],
              organizationId: null
            }));
            this.loading = false;
            this.router.navigate(['/dashboard']);
          }
        });
      },
      error: (err) => {
        this.error = err.error?.message || 'Identifiants invalides';
        this.loading = false;
      }
    });
  }
}
