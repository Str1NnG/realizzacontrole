import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { ToastService } from '../../services/toast.service';
import { ConfirmModalService } from '../../services/confirm-modal.service';
import { AuthService } from '../../services/auth.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-financeiro',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './financeiro.component.html'
})
export class FinanceiroComponent implements OnInit, OnDestroy {
  allRegistros: any[] = [];
  registrosFiltrados: any[] = [];
  totalPendente = 0;
  totalRecebido = 0;
  statusFilter: 'TODOS' | 'PENDENTE' | 'PAGO' = 'TODOS';
  searchQuery = '';
  
  dataInicio = ''; 
  dataFim = '';    
  isLoading = false;
  operadores: any[] = [];
  clientes: any[] = [];
  isManualModalOpen = false;
  novoRegistro: any = {};
  selectedIds: Set<number> = new Set();
  
  recibosToPrint: any[] = [];
  nomeEmpresaUsuario = 'REALIZZA MAIS ENGENHARIA';
  private userSub!: Subscription;

  isEditModalOpen = false;
  registroEmEdicao: any = null;

  constructor(
    private apiService: ApiService,
    private toastService: ToastService,
    private confirmService: ConfirmModalService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {

    this.apiService.getOperadores().subscribe(res => this.operadores = res);
    this.apiService.getClientes().subscribe(res => this.clientes = res);
    // 1. Vai buscar o nome da empresa do utilizador logado
    this.userSub = this.authService.currentUser.subscribe(user => {
      if (user && user.nomeEmpresa) this.nomeEmpresaUsuario = user.nomeEmpresa;
    });

    // 2. Preenche as datas automaticamente (Do dia 1º do mês até hoje) para o filtro não esconder os dados
    const hoje = new Date();
    const primeiroDiaDoMes = new Date(hoje.getFullYear(), hoje.getMonth(), 1);
    this.dataInicio = this.formatDate(primeiroDiaDoMes);
    this.dataFim = this.formatDate(hoje);

    // 3. Força a ir buscar os dados reais do Banco de Dados (isto resolve o problema do "fantasma" ao excluir noutra tela)
    this.loadData();
  }

  ngOnDestroy(): void {
    if (this.userSub) this.userSub.unsubscribe();
  }

  private formatDate(date: Date): string {
    const offset = date.getTimezoneOffset() * 60000;
    return new Date(date.getTime() - offset).toISOString().split('T')[0];
  }
  

  loadData(): void {
    this.isLoading = true;
    this.apiService.getAllRegistros().subscribe({
      next: (data) => {
        // A sua lógica original que calcula os valores corretamente!
        this.allRegistros = data.map((reg: any) => {
          const horas = Math.max(0, (reg.horimetroFinal || 0) - (reg.horimetroInicial || 0));
          const vHora = reg.clienteDetalhe?.valorHora || 200.0;
          const vFaturado = (reg.valorFaturado && reg.valorFaturado > 0) ? reg.valorFaturado : (horas * vHora);
          return { ...reg, valorCalculadoFinal: vFaturado };
        }).sort((a: any, b: any) => new Date(b.data).getTime() - new Date(a.data).getTime());
        
        // Aplica os filtros para mostrar na tabela
        this.applyFilters();
        this.isLoading = false;
      },
      error: () => {
        this.toastService.error('Erro ao carregar os dados financeiros.');
        this.isLoading = false;
      }
    });
  }

  applyFilters(): void {
    if (!this.dataInicio || !this.dataFim) return;

    const start = new Date(`${this.dataInicio}T00:00:00`).getTime();
    const end = new Date(`${this.dataFim}T23:59:59`).getTime();
    
    this.registrosFiltrados = this.allRegistros.filter(reg => {
      const regTime = new Date(`${reg.data}T12:00:00`).getTime();
      const matchDate = regTime >= start && regTime <= end;
      const matchStatus = this.statusFilter === 'TODOS' || reg.statusPagamento === this.statusFilter;
      const term = this.searchQuery.toLowerCase();
      return matchDate && matchStatus && (!term || reg.operador?.nome?.toLowerCase().includes(term) || reg.cliente?.toLowerCase().includes(term) || reg.clienteDetalhe?.nome?.toLowerCase().includes(term));
    });
    
    this.totalPendente = 0; 
    this.totalRecebido = 0;
    
    this.registrosFiltrados.forEach(r => {
      if (r.statusPagamento === 'PAGO') this.totalRecebido += r.valorCalculadoFinal;
      else this.totalPendente += r.valorCalculadoFinal;
    });
    
    this.selectedIds.clear();
  }

  toggleSelection(id: number): void { this.selectedIds.has(id) ? this.selectedIds.delete(id) : this.selectedIds.add(id); }
  
  toggleAll(event: any): void {
    if (event.target.checked) this.registrosFiltrados.forEach(r => { if (r.statusPagamento === 'PENDENTE') this.selectedIds.add(r.id); });
    else this.selectedIds.clear();
  }
  
  isAllSelected(): boolean { 
    const p = this.registrosFiltrados.filter(r => r.statusPagamento === 'PENDENTE');
    return p.length > 0 && p.every(r => this.selectedIds.has(r.id));
  }

  marcarComoPago(): void {
    if (this.selectedIds.size === 0) return;
    this.confirmService.open(`Confirmar recebimento de ${this.selectedIds.size} itens?`, 'Baixa Financeira').subscribe(c => {
      if (c) {
        this.apiService.marcarComoPago(Array.from(this.selectedIds)).subscribe({
          next: () => { this.toastService.success('Valores atualizados!'); this.loadData(); },
          error: () => this.isLoading = false
        });
      }
    });
  }

  abrirModalEdicao(reg: any): void {
    this.registroEmEdicao = { ...reg };
    this.isEditModalOpen = true;
  }

  salvarEdicao(): void {
    if (!this.registroEmEdicao) return;
    this.isLoading = true;
    const horas = Math.max(0, (this.registroEmEdicao.horimetroFinal || 0) - (this.registroEmEdicao.horimetroInicial || 0));
    const vHora = this.registroEmEdicao.clienteDetalhe?.valorHora || 200.0;
    this.registroEmEdicao.valorFaturado = horas * vHora;

    this.apiService.updateRegistro(this.registroEmEdicao.id, this.registroEmEdicao).subscribe({
      next: () => {
        this.toastService.success('Serviço atualizado com sucesso!');
        this.isEditModalOpen = false;
        this.loadData();
      },
      error: () => {
        this.toastService.error('Erro ao atualizar serviço.');
        this.isLoading = false;
      }
    });
  }

  formatarRecibo(reg: any) {
    const horas = Math.max(0, (reg.horimetroFinal || 0) - (reg.horimetroInicial || 0));
    return {
      id: reg.id,
      cliente: reg.cliente || reg.clienteDetalhe?.nome || 'Não Informado',
      local: reg.clienteDetalhe?.localServico || 'Não Especificado',
      servico: reg.servico || 'Serviços de Terraplanagem',
      operador: reg.operador?.nome || 'NÃO DEFINIDO',
      maquina: reg.operador?.maquina?.nome || 'NÃO DEFINIDA',
      horas: horas,
      valorHora: horas > 0 ? (reg.valorCalculadoFinal / horas) : 0,
      total: reg.valorCalculadoFinal,
      emissao: new Date(),
      latitude: reg.latitude,
      longitude: reg.longitude
    };
  }

  imprimirRecibo(modo: 'unico' | 'lote', reg?: any): void {
    if (modo === 'unico' && reg) {
      this.recibosToPrint = [this.formatarRecibo(reg)];
    } else if (modo === 'lote') {
      const selecionados = this.registrosFiltrados.filter(r => this.selectedIds.has(r.id));
      this.recibosToPrint = selecionados.map(r => this.formatarRecibo(r));
    }
    
    setTimeout(() => { window.print(); }, 1500);
  }

  exportarExcel(): void {
    const header = ['Data', 'Operador', 'Cliente', 'Horas', 'Valor do Serviço'];
    const rows = this.registrosFiltrados.map(r => [r.data, r.operador?.nome, r.cliente || r.clienteDetalhe?.nome, (r.horimetroFinal - r.horimetroInicial).toFixed(1), r.valorCalculadoFinal.toFixed(2)]);
    const csvContent = "data:text/csv;charset=utf-8,\uFEFF" + [header.join(";"), ...rows.map(e => e.join(";"))].join("\n");
    const link = document.createElement("a");
    link.setAttribute("href", encodeURI(csvContent));
    link.setAttribute("download", `financeiro_${this.dataInicio}.csv`);
    document.body.appendChild(link);
    link.click();
  }

  abrirModalManual(): void {
    this.novoRegistro = { 
      data: this.formatDate(new Date()), 
      statusPagamento: 'PENDENTE',
      servico: 'PRESTAÇÃO DE SERVIÇO'
    };
    this.isManualModalOpen = true;
  }

  salvarLancamentoManual(): void {
    if (!this.novoRegistro.operadorId) {
      this.toastService.error('Selecione um operador obrigatóriamente.');
      return;
    }

    this.isLoading = true;
    const payload = {
      data: this.novoRegistro.data,
      servico: this.novoRegistro.servico,
      horimetroInicial: this.novoRegistro.horimetroInicial,
      horimetroFinal: this.novoRegistro.horimetroFinal,
      valorFaturado: this.novoRegistro.valorFaturado,
      statusPagamento: this.novoRegistro.statusPagamento,
      operador: { id: this.novoRegistro.operadorId },
      clienteEntity: this.novoRegistro.clienteId ? { id: this.novoRegistro.clienteId } : null
    };

    this.apiService.createRegistroManual(payload).subscribe({
      next: () => {
        this.toastService.success('Lançamento manual guardado com sucesso!');
        this.isManualModalOpen = false;
        this.loadData();
      },
      error: () => {
        this.toastService.error('Erro ao guardar lançamento manual.');
        this.isLoading = false;
      }
    });
  }
}