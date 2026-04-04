import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';

// Define os tipos de toast que podemos ter
export type ToastType = 'success' | 'error' | 'info' | 'warning';

// A "carga" que o serviço vai enviar
export interface ToastMessage {
  message: string;
  type: ToastType;
  duration: number; // Em milissegundos
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {

  // O Subject que vai "disparar" os toasts
  private toastSubject = new Subject<ToastMessage>();
  
  // O Observable que o componente vai "ouvir"
  public toast$ = this.toastSubject.asObservable();

  constructor() { }

  /**
   * Método principal para mostrar um toast.
   * @param message A mensagem a ser exibida.
   * @param type O tipo (success, error, etc.) para controlar a cor.
   * @param duration Quanto tempo ele fica na tela (padrão: 3 segundos).
   */
  show(message: string, type: ToastType = 'info', duration: number = 3000): void {
    this.toastSubject.next({ message, type, duration });
  }

  // Funções de atalho (para facilitar a vida)
  success(message: string, duration: number = 3000): void {
    this.show(message, 'success', duration);
  }

  error(message: string, duration: number = 5000): void { // Erros ficam mais tempo
    this.show(message, 'error', duration);
  }
  
  info(message: string, duration: number = 3000): void {
    this.show(message, 'info', duration);
  }
}