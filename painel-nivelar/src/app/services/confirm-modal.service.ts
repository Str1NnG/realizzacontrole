import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { take } from 'rxjs/operators';

interface ConfirmModalState {
  visible: boolean;
  message: string;
  title: string;
}

@Injectable({
  providedIn: 'root'
})
export class ConfirmModalService {

  // Este Subject comanda o componente (o que mostrar)
  private state$ = new Subject<ConfirmModalState>();
  
  // Este Subject envia a resposta de volta para quem perguntou (true/false)
  private result$ = new Subject<boolean>();

  constructor() { }

  /**
   * Chamado pelo componente (ex: Dashboard) para abrir o modal.
   * Retorna um Observable que emite 'true' (Confirmar) ou 'false' (Cancelar).
   */
  open(message: string, title: string = 'Confirmar Ação'): Observable<boolean> {
    
    // Envia o comando para o componente da UI abrir
    this.state$.next({
      visible: true,
      message: message,
      title: title
    });

    // Retorna o 'result$' para quem chamou, mas com 'take(1)'
    // para que a inscrição seja finalizada após o primeiro clique.
    return this.result$.pipe(take(1));
  }

  /** Chamado pelo ConfirmModalComponent quando o usuário clica em 'Confirmar' */
  confirm(): void {
    this.state$.next({ visible: false, message: '', title: '' }); // Fecha o modal
    this.result$.next(true); // Envia 'true' para quem perguntou
  }

  /** Chamado pelo ConfirmModalComponent quando o usuário clica em 'Cancelar' */
  cancel(): void {
    this.state$.next({ visible: false, message: '', title: '' }); // Fecha o modal
    this.result$.next(false); // Envia 'false' para quem perguntou
  }

  /** Usado pelo ConfirmModalComponent para "ouvir" os comandos de abertura */
  getState(): Observable<ConfirmModalState> {
    return this.state$.asObservable();
  }
}