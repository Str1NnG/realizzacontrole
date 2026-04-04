import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';
import { ConfirmModalService } from '../../services/confirm-modal.service';

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './cliente.component.html'
})
export class ClientesComponent implements OnInit {
  clientes: any[] = [];
  servicosRealizados: any[] = [];

  termoBusca = '';
  dropdownAberto = false;
  servicoVinculado: any = null;

  isLoading = false;
  isModalOpen = false;
  isEditing = false;
  clienteIdEmEdicao: number | null = null;

  clienteForm: FormGroup;

  constructor(
    private apiService: ApiService,
    private toastService: ToastService,
    private confirmService: ConfirmModalService,
    private fb: FormBuilder,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {
    this.clienteForm = this.fb.group({
      nome: ['', Validators.required],
      localServico: ['', Validators.required],
      valorHora: [200, [Validators.required, Validators.min(1)]],
      latitude: [{ value: null, disabled: true }, Validators.required],
      longitude: [{ value: null, disabled: true }, Validators.required],
      raioTolerancia: [50, [Validators.required, Validators.min(1)]]
    });
  }

  ngOnInit(): void {
    this.loadClientes();
    this.loadHistoricoServicos();
  }

  // ─── Filtragem do dropdown ────────────────────────────────────────────────
  get servicosFiltrados(): any[] {
    const t = this.termoBusca.trim().toLowerCase();
    if (!t) return this.servicosRealizados;
    return this.servicosRealizados.filter(s => {
      const label = [
        String(s.id),
        s.operador?.nome || s.nomeOperador || '',
        s.maquina?.nome || '',
        s.enderecoFormatado || ''
      ].join(' ').toLowerCase();
      return label.includes(t);
    });
  }

  // ─── Clientes ─────────────────────────────────────────────────────────────
  loadClientes(): void {
    this.isLoading = true;
    this.apiService.getClientes().subscribe({
      next: (data) => {
        this.clientes = data.sort((a, b) => a.nome.localeCompare(b.nome));
        this.isLoading = false;
        // Geocodifica os endereços dos clientes existentes para exibir na tabela
        this.geocodificarEmLotes(this.clientes, 5);
      },
      error: () => {
        this.toastService.error('Erro ao carregar clientes.');
        this.isLoading = false;
      }
    });
  }

  // ─── Histórico de serviços ────────────────────────────────────────────────
  loadHistoricoServicos(): void {
    this.apiService.getHistoricoServicos().subscribe({
      next: (dados) => {
        this.servicosRealizados = dados
          .filter((s: any) => s.latitude && s.longitude)
          .sort((a: any, b: any) => b.id - a.id);
        this.geocodificarEmLotes(this.servicosRealizados, 5);
      }
    });
  }

  // ─── Geocodificação em lotes (reutilizável para clientes e serviços) ──────
  private geocodificarEmLotes(lista: any[], loteSize: number): void {
    const processar = (index: number) => {
      if (index >= lista.length) return;
      lista.slice(index, index + loteSize).forEach(item => this.buscarEndereco(item));
      setTimeout(() => processar(index + loteSize), 1200);
    };
    processar(0);
  }

  /**
   * Geocodificação reversa com 3 camadas de fallback.
   * Funciona tanto para itens de `clientes` quanto de `servicosRealizados`
   * — ambos têm `latitude` e `longitude`.
   */
  buscarEndereco(item: any): void {
    if (!item.latitude || !item.longitude) return;
    const lat = item.latitude;
    const lon = item.longitude;

    // ── 1ª tentativa: BigDataCloud ──────────────────────────────────────────
    const urlBDC = `https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=${lat}&longitude=${lon}&localityLanguage=pt`;
    this.http.get<any>(urlBDC).subscribe({
      next: (res) => {
        item.enderecoFormatado = this.montarEnderecoFromBDC(res, lat, lon);
        item.enderecoCarregado = true;
        this.cdr.detectChanges();
      },
      error: () => this.tentarNominatim(item, lat, lon)
    });
  }

  private montarEnderecoFromBDC(res: any, lat: number, lon: number): string {
    const infoList: any[] = res.localityInfo?.informative || [];
    const adminList: any[] = res.localityInfo?.administrative || [];

    // Rua: procura nos campos informativos
    const rua = infoList.find(i => i.description === 'road')?.name
      || infoList.find(i => i.description === 'hamlet')?.name
      || '';

    const numero = res.streetNumber ? `nº ${res.streetNumber}` : '';

    // Cidade: nível admin 8 (município) ou fallback para os campos raiz
    const cidade = adminList.find(a => a.adminLevel === 8)?.name
      || res.city
      || res.locality
      || '';

    // Estado: nível admin 4 ou principalSubdivision
    const estado = adminList.find(a => a.adminLevel === 4)?.name
      || res.principalSubdivision
      || '';

    const partes = [rua, numero, cidade, estado].filter(Boolean);
    return partes.length ? partes.join(', ') : `${lat}, ${lon}`;
  }

  private tentarNominatim(item: any, lat: number, lon: number): void {
    // ── 2ª tentativa: Nominatim ─────────────────────────────────────────────
    const urlNom = `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lon}&accept-language=pt`;
    this.http.get<any>(urlNom).subscribe({
      next: (res) => {
        const a = res.address;
        const rua    = a.road || a.pedestrian || a.path || a.hamlet || '';
        const numero = a.house_number ? `nº ${a.house_number}` : '';
        const cidade = a.city || a.town || a.village || a.municipality || '';
        const estado = a.state || '';
        const partes = [rua, numero, cidade, estado].filter(Boolean);
        item.enderecoFormatado = partes.length ? partes.join(', ') : `${lat}, ${lon}`;
        item.enderecoCarregado = true;
        this.cdr.detectChanges();
      },
      error: () => {
        // ── 3ª tentativa: coordenadas brutas ───────────────────────────────
        item.enderecoFormatado = `${lat}, ${lon}`;
        item.enderecoCarregado = true;
        this.cdr.detectChanges();
      }
    });
  }

  // ─── Dropdown de serviços ─────────────────────────────────────────────────
  abrirDropdown(): void { this.dropdownAberto = true; }
  fecharDropdown(): void { setTimeout(() => { this.dropdownAberto = false; }, 180); }

  selecionarServico(servico: any): void {
    this.servicoVinculado = servico;
    this.termoBusca = '';
    this.dropdownAberto = false;
    this.clienteForm.patchValue({
      latitude: servico.latitude,
      longitude: servico.longitude,
      localServico: servico.enderecoFormatado || `Serviço #${servico.id}`
    });
  }

  limparVinculo(): void {
    this.servicoVinculado = null;
    this.clienteForm.patchValue({ latitude: null, longitude: null, localServico: '' });
  }

  nomeOperador(s: any): string { return s.operador?.nome || s.nomeOperador || 'Operador'; }
  nomeMaquina(s: any): string  { return s.maquina?.nome || 'Sem máquina'; }

  // ─── Modal ────────────────────────────────────────────────────────────────
  abrirModalNovo(): void {
    this.isEditing = false;
    this.clienteIdEmEdicao = null;
    this.servicoVinculado = null;
    this.termoBusca = '';
    this.clienteForm.reset({ valorHora: 200, raioTolerancia: 50 });
    this.clienteForm.get('latitude')?.disable();
    this.clienteForm.get('longitude')?.disable();
    this.isModalOpen = true;
  }

  abrirModalEdicao(cliente: any): void {
    this.isEditing = true;
    this.clienteIdEmEdicao = cliente.id;
    this.servicoVinculado = null;
    this.termoBusca = '';
    this.clienteForm.patchValue({
      nome: cliente.nome,
      localServico: cliente.localServico,
      valorHora: cliente.valorHora,
      latitude: cliente.latitude,
      longitude: cliente.longitude,
      raioTolerancia: cliente.raioTolerancia || 50
    });
    this.isModalOpen = true;
  }

  fecharModal(): void { this.isModalOpen = false; }

  salvarCliente(): void {
    if (this.clienteForm.invalid) {
      this.toastService.error('Preencha os campos obrigatórios.');
      return;
    }
    this.isLoading = true;
    const dados = this.clienteForm.getRawValue();
    const request = this.isEditing && this.clienteIdEmEdicao
      ? this.apiService.updateCliente(this.clienteIdEmEdicao, dados)
      : this.apiService.createCliente(dados);

    request.subscribe({
      next: () => {
        this.toastService.success('Cliente salvo com sucesso!');
        this.fecharModal();
        this.loadClientes();
      },
      error: () => {
        this.toastService.error('Erro ao salvar cliente.');
        this.isLoading = false;
      }
    });
  }

  excluirCliente(id: number, nome: string): void {
    this.confirmService.open(`Deseja excluir o cliente ${nome}?`, 'Confirmar Exclusão').subscribe(confirm => {
      if (confirm) {
        this.apiService.deleteCliente(id).subscribe({
          next: () => {
            this.toastService.success('Cliente removido.');
            this.loadClientes();
          },
          error: () => this.toastService.error('Erro ao excluir cliente.')
        });
      }
    });
  }
}