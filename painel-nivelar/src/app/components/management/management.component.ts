import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { ModalComponent } from '../../components/modal/modal.component';
import { ToastService } from '../../services/toast.service'; 

@Component({
  selector: 'app-management',
  standalone: true,
  imports: [CommonModule, FormsModule, ModalComponent],
  templateUrl: './management.component.html'
})
export class ManagementComponent implements OnInit {
  activeTab: 'operadores' | 'maquinas' = 'operadores';
  operadores: any[] = [];
  maquinas: any[] = [];

  isModalOpen = false;
  isEditing = false;
  currentItem: any = null;
  isLoading = false; 

  constructor(
    private apiService: ApiService,
    private authService: AuthService, 
    private toastService: ToastService 
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.apiService.getAllOperadores().subscribe({
        next: (data: any) => this.operadores = data,
        error: (err: any) => this.toastService.error(`Erro ao carregar operadores: ${err.message}`) 
    });
    this.apiService.getAllMaquinas().subscribe({
        next: (data: any) => this.maquinas = data, 
        error: (err: any) => this.toastService.error(`Erro ao carregar máquinas: ${err.message}`) 
    });
  }

  openModal(item: any | null = null): void {
    this.isEditing = !!item;
    this.currentItem = item ? JSON.parse(JSON.stringify(item)) : {};

    if (this.activeTab === 'operadores' && !this.currentItem.maquina) {
        this.currentItem.maquina = { id: null };
    } else if (this.activeTab === 'operadores' && this.currentItem.maquina && this.currentItem.maquina.id === undefined) {
         this.currentItem.maquina.id = null;
    }

    if (this.activeTab === 'operadores' && !this.isEditing) {
      this.currentItem.cargo = 'ROLE_OPERADOR';
    }

    this.isModalOpen = true;
  }

  closeModal(): void {
    this.isModalOpen = false;
    this.currentItem = null;
    this.isLoading = false; 
  }

  saveItem(): void {
    if (!this.currentItem) return;
    this.isLoading = true;

    let itemToSend = { ...this.currentItem };

    if (this.activeTab === 'operadores' && itemToSend.maquina) {
      if (itemToSend.maquina.id === null || itemToSend.maquina.id === 'null' || itemToSend.maquina.id === '') {
        itemToSend.maquina = null;
      } else {
         itemToSend.maquina = { id: Number(itemToSend.maquina.id) };
      }
    } else if (this.activeTab === 'operadores' && !itemToSend.maquina) {
        itemToSend.maquina = null;
    }

    // --- CORREÇÃO DA LÓGICA DA SENHA ---
    // Se estiver a editar, só elimina a senha do JSON se ela estiver vazia.
    // Se o utilizador digitou algo, ela permanece no pacote (itemToSend).
    if (this.isEditing) {
        if (!itemToSend.senha || itemToSend.senha.trim() === '') {
            delete itemToSend.senha;
        }
    }

    // --- LIMPEZA DE SEGURANÇA BASEADA NO CARGO ---
    if (this.activeTab === 'operadores') {
      if (itemToSend.cargo === 'ROLE_ADMIN') {
        itemToSend.maquina = null; // Admin não tem máquina
      } else if (itemToSend.cargo === 'ROLE_OPERADOR') {
        delete itemToSend.senha; // Operador loga por CPF, não enviamos senha
      }
    }

    if (this.isEditing && itemToSend.senha !== undefined) {
        if (!itemToSend.senha || itemToSend.senha.trim() === '') {
            delete itemToSend.senha;
        }
    }

    let apiCall = this.activeTab === 'operadores'
      ? (this.isEditing ? this.apiService.updateOperador(itemToSend.id, itemToSend) : this.apiService.createOperador(itemToSend))
      : (this.isEditing ? this.apiService.updateMaquina(itemToSend.id, itemToSend) : this.apiService.createMaquina(itemToSend));

    apiCall.subscribe({
      next: () => {
        this.toastService.success(`${this.activeTab === 'operadores' ? 'Operador' : 'Máquina'} salvo(a) com sucesso!`);
        this.loadData(); 
        this.closeModal(); 
      },
      error: (err: any) => { 
        this.toastService.error(err.message || 'Erro ao salvar os dados.');
        this.isLoading = false; 
      }
    });
  }

  deleteItem(id: number): void {
    if (!confirm('Tem certeza que deseja deletar este item? Esta ação não pode ser desfeita.')) return;

    const apiCall = this.activeTab === 'operadores'
      ? this.apiService.deleteOperador(id)
      : this.apiService.deleteMaquina(id);

    apiCall.subscribe({
      next: () => {
         this.toastService.success('Item deletado com sucesso!');
         this.loadData(); 
      },
      error: (err: any) => { 
        this.toastService.error(err.message || 'Erro ao deletar. Verifique se o item não está em uso em relatórios.');
      }
    });
  }
}