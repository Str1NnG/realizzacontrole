import { Component } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  cpf = '';
  senha = '';
  errorMessage = '';
  isLoading = false;
  showPassword = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private toastService: ToastService
  ) {}

  // Formata o CPF em tempo real: 000.000.000-00
  onCpfInput(event: any): void {
    let val = event.target.value.replace(/\D/g, '');
    if (val.length > 11) val = val.substring(0, 11);

    if (val.length > 9) {
      val = val.replace(/^(\d{3})(\d{3})(\d{3})(\d{2}).*/, '$1.$2.$3-$4');
    } else if (val.length > 6) {
      val = val.replace(/^(\d{3})(\d{3})(\d{0,3}).*/, '$1.$2.$3');
    } else if (val.length > 3) {
      val = val.replace(/^(\d{3})(\d{0,3}).*/, '$1.$2');
    }
    this.cpf = val;
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {
    const cpfLimpo = this.cpf.replace(/\D/g, '');
    
    if (cpfLimpo.length !== 11) {
      this.toastService.error('CPF incompleto.');
      return;
    }

    this.errorMessage = '';
    this.isLoading = true;

    this.authService.login(cpfLimpo, this.senha).subscribe({
      next: (loginSuccess) => {
        this.isLoading = false;
        if (loginSuccess) {
          const userRole = this.authService.getUserRole();
          if (userRole === 'ROLE_ADMIN') {
            this.router.navigate(['/dashboard']);
            this.toastService.success('Bem-vindo ao Realizza Controle!');
          } else {
            this.authService.logout();
            this.toastService.error('Acesso restrito a administradores.');
          }
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.toastService.error(err.error?.message || 'Erro ao realizar login.');
      }
    });
  }
}