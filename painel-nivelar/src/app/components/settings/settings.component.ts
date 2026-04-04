import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './settings.component.html'
})
export class SettingsComponent implements OnInit {
  empresaForm: FormGroup;
  perfilForm: FormGroup;
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private toastService: ToastService
  ) {
    this.empresaForm = this.fb.group({
      nomeEmpresa: ['Realizza Controle', Validators.required],
      cnpj: ['12.345.678/0001-99'],
      valorHoraPadrao: [200, Validators.required]
    });

    this.perfilForm = this.fb.group({
      nome: ['Administrador', Validators.required],
      email: ['admin@realizza.com', [Validators.required, Validators.email]],
      senhaAtual: [''],
      novaSenha: ['']
    });
  }

  ngOnInit(): void {}

  salvarEmpresa(): void {
    if (this.empresaForm.invalid) return;
    this.isLoading = true;
    setTimeout(() => {
      this.toastService.success('Dados da empresa atualizados com sucesso!');
      this.isLoading = false;
    }, 800);
  }

  salvarPerfil(): void {
    if (this.perfilForm.invalid) return;
    this.isLoading = true;
    setTimeout(() => {
      this.toastService.success('Perfil atualizado com sucesso!');
      this.perfilForm.patchValue({ senhaAtual: '', novaSenha: '' });
      this.isLoading = false;
    }, 800);
  }
}