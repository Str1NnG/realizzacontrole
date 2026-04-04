import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { ConfirmModalService } from '../../services/confirm-modal.service';
import { ModalComponent } from '../modal/modal.component'; // <-- REUTILIZANDO NOSSO MODAL!

@Component({
  selector: 'app-confirm-modal',
  standalone: true,
  imports: [CommonModule, ModalComponent], // <-- IMPORTA O APP-MODAL
  templateUrl: './confirm-modal.component.html'
  // Sem 'styleUrls'
})
export class ConfirmModalComponent implements OnInit, OnDestroy {

  isVisible = false;
  title = 'Confirmar Ação';
  message = 'Você tem certeza?';
  private stateSubscription: Subscription | undefined;

  constructor(private confirmService: ConfirmModalService) {}

  ngOnInit(): void {
    // Fica "ouvindo" o serviço para saber quando deve aparecer
    this.stateSubscription = this.confirmService.getState().subscribe(state => {
      this.isVisible = state.visible;
      this.title = state.title;
      this.message = state.message;
    });
  }

  ngOnDestroy(): void {
    if (this.stateSubscription) {
      this.stateSubscription.unsubscribe();
    }
  }

  onConfirm(): void {
    this.confirmService.confirm();
  }

  onCancel(): void {
    this.confirmService.cancel();
  }
}