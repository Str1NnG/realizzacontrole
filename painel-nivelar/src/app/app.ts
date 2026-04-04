import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './services/auth.service';
import { ConfirmModalComponent } from './components/confirm-modal/confirm-modal.component'; 
// 1. IMPORTE O NOVO COMPONENTE DE TOAST
import { ToastComponent } from './components/toast/toast.component';

@Component({
  selector: 'app-root',
  standalone: true,
  // 2. ADICIONE O 'ToastComponent' AOS IMPORTS
  imports: [
    RouterOutlet, 
    CommonModule, 
    ConfirmModalComponent,
    ToastComponent // <-- ADICIONADO AQUI
  ],
  templateUrl: './app.html'
})
export class App {
  constructor(public authService: AuthService) {}
}