import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { ToastService, ToastMessage } from '../../services/toast.service';

// Interface interna para rastrear toasts com um ID
interface Toast extends ToastMessage {
  id: number;
}

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './toast.component.html'
  // Sem 'styleUrls'
})
export class ToastComponent implements OnInit, OnDestroy {

  toasts: Toast[] = [];
  private toastSubscription: Subscription | undefined;
  private toastIdCounter = 0;

  constructor(private toastService: ToastService) {}

  ngOnInit(): void {
    // Fica "ouvindo" o serviço...
    this.toastSubscription = this.toastService.toast$.subscribe(
      (toastMessage: ToastMessage) => {
        this.addToast(toastMessage);
      }
    );
  }

  ngOnDestroy(): void {
    if (this.toastSubscription) {
      this.toastSubscription.unsubscribe();
    }
  }

  private addToast(toastMessage: ToastMessage): void {
    const id = this.toastIdCounter++;
    const newToast: Toast = { id, ...toastMessage };
    
    this.toasts.push(newToast);

    // Agenda a remoção do toast
    setTimeout(() => {
      this.removeToast(id);
    }, toastMessage.duration);
  }

  // Remove o toast (chamado pelo timer ou pelo botão de fechar)
  removeToast(id: number): void {
    this.toasts = this.toasts.filter(toast => toast.id !== id);
  }

  // Função 'trackBy' para o Angular otimizar o *ngFor
  trackByToast(index: number, toast: Toast): number {
    return toast.id;
  }
}