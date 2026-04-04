import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions, Chart, registerables } from 'chart.js';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { Router, RouterModule } from '@angular/router';
import { ToastService } from '../../services/toast.service';
import { ConfirmModalService } from '../../services/confirm-modal.service';
import { ModalComponent } from '../../components/modal/modal.component';
import { DomSanitizer, SafeResourceUrl, SafeUrl } from '@angular/platform-browser';
import { Subscription } from 'rxjs';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [ CommonModule, FormsModule, BaseChartDirective, RouterModule, ModalComponent ],
  templateUrl: './dashboard.component.html',
  providers: [DatePipe]
})
export class DashboardComponent implements OnInit, OnDestroy {
  nomeEmpresa = 'REALIZZA MAIS ENGENHARIA';
  private userSub!: Subscription;
  
  allRegistros: any[] = [];
  registrosFiltrados: any[] = [];
  dataInicio = '';
  dataFim = '';
  searchQuery = '';
  isLoading = false;
  isExportingPdf = false;
  
  // Toggle dos gráficos
  analysisType: 'funcionarios' | 'maquinas' = 'funcionarios';
  evolutionType: 'line' | 'bar' = 'line';

  resumo = { totalRegistros: 0, totalHoras: 0, totalOperadores: 0, mediaDiaria: 0 };

  // Modais
  isEditModalVisible = false;
  currentRegistro: any = null;
  
  // --- VARIÁVEIS DO NOVO SISTEMA DE GALERIA ---
  isAnexoModalVisible = false;
  currentFotoRegistro: any = null;
  activePhotoTab: 'inicio' | 'final' | 'unica' = 'inicio';
  isLoadingAnexoInicio = false;
  isLoadingAnexoFinal = false;
  anexoInicioUrl: SafeUrl | null = null;
  anexoFinalUrl: SafeUrl | null = null;
  anexoUnicaUrl: SafeUrl | null = null;
  
  isMapModalVisible = false;
  mapUrl: SafeResourceUrl | null = null;

  // --- GRÁFICOS ---
  valoresEvolucao: number[] = [];
  labelsEvolucao: string[] = [];
  public evolutionChartData: any;
  public evolutionChartOptions: any;

  public doughnutChartData: ChartData<'doughnut'> | undefined;
  public doughnutChartOptions: ChartOptions<'doughnut'> = {
    responsive: true, maintainAspectRatio: false, cutout: '75%',
    plugins: { 
      legend: { display: false },
      tooltip: { backgroundColor: 'rgba(15, 23, 42, 0.9)', padding: 12, cornerRadius: 8 }
    }
  };

  constructor(
    private apiService: ApiService,
    private authService: AuthService,
    private toastService: ToastService,
    private confirmService: ConfirmModalService,
    private datePipe: DatePipe,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.userSub = this.authService.currentUser.subscribe(user => {
      if (user && user.nomeEmpresa) this.nomeEmpresa = user.nomeEmpresa;
    });

    const hoje = new Date();
    const inicio = new Date(hoje.getFullYear(), hoje.getMonth(), 1); // Primeiro dia do mês
    this.dataInicio = this.formatDate(inicio);
    this.dataFim = this.formatDate(hoje);
    
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
        this.allRegistros = data.map(r => {
          const hIni = r.horimetroInicial || 0;
          const hFim = r.horimetroFinal || 0;
          return { ...r, calculatedHorasTrabalhadas: Math.max(0, hFim - hIni) };
        }).sort((a, b) => new Date(b.data).getTime() - new Date(a.data).getTime());
        
        this.runAnalysis();
      },
      error: (err) => {
        this.toastService.error('Falha ao carregar dados.');
        this.isLoading = false;
        if (err.status === 403) this.authService.logout();
      }
    });
  }

  runAnalysis(): void {
    if (!this.dataInicio || !this.dataFim) return;

    const start = new Date(`${this.dataInicio}T00:00:00`).getTime();
    const end = new Date(`${this.dataFim}T23:59:59`).getTime();
    const term = this.searchQuery.toLowerCase();

    this.registrosFiltrados = this.allRegistros.filter(reg => {
      const regTime = new Date(`${reg.data}T12:00:00`).getTime();
      const matchDate = regTime >= start && regTime <= end;
      const matchSearch = !term || 
                          reg.operador?.nome?.toLowerCase().includes(term) || 
                          reg.servico?.toLowerCase().includes(term) || 
                          reg.cliente?.toLowerCase().includes(term) || 
                          reg.clienteDetalhe?.nome?.toLowerCase().includes(term);
      return matchDate && matchSearch;
    });

    const registrosParaGraficos = this.allRegistros.filter(reg => {
      const regTime = new Date(`${reg.data}T12:00:00`).getTime();
      return regTime >= start && regTime <= end;
    });

    this.resumo.totalRegistros = registrosParaGraficos.length;
    this.resumo.totalHoras = registrosParaGraficos.reduce((sum, r) => sum + r.calculatedHorasTrabalhadas, 0);
    const operadores = new Set(registrosParaGraficos.map(r => r.operador?.id).filter(id => id));
    this.resumo.totalOperadores = operadores.size;

    this.carregarEvolucaoDiaria(registrosParaGraficos);
    this.carregarGraficosSecundarios();
  }

  carregarEvolucaoDiaria(registrosNoPeriodo: any[]): void {
    const labels = this.gerarArrayDeDatas(this.dataInicio, this.dataFim);
    let diasAtivos = 0;

    const values = labels.map(label => {
      const recordsForDay = registrosNoPeriodo.filter(r => r.data === label);
      const horasDia = recordsForDay.reduce((sum, r) => sum + r.calculatedHorasTrabalhadas, 0);
      if (horasDia > 0) diasAtivos++;
      return horasDia;
    });

    this.resumo.mediaDiaria = diasAtivos > 0 ? (this.resumo.totalHoras / diasAtivos) : 0;
    this.labelsEvolucao = labels.map(l => `${l.split('-')[2]}/${l.split('-')[1]}`);
    this.valoresEvolucao = values;
    this.atualizarGraficoEvolucao();
  }

  toggleEvolutionType(type: 'line' | 'bar'): void {
    if (this.evolutionType !== type) {
      this.evolutionType = type;
      this.atualizarGraficoEvolucao();
    }
  }

  atualizarGraficoEvolucao(): void {
    this.evolutionChartData = {
      labels: this.labelsEvolucao,
      datasets: [{
        data: this.valoresEvolucao, 
        label: 'Horas Trabalhadas', 
        borderColor: '#3b82f6', 
        backgroundColor: this.evolutionType === 'line' ? 'rgba(59, 130, 246, 0.1)' : '#3b82f6',
        fill: this.evolutionType === 'line',
        borderWidth: this.evolutionType === 'line' ? 3 : 0,
        pointBackgroundColor: '#3b82f6',
        pointRadius: this.evolutionType === 'line' ? 4 : 0,
        borderRadius: this.evolutionType === 'bar' ? 4 : 0,
        maxBarThickness: 40
      }]
    };

    this.evolutionChartOptions = {
      responsive: true, maintainAspectRatio: false,
      elements: { line: { tension: 0.4 } },
      scales: {
        y: { beginAtZero: true, border: { display: false } },
        x: { grid: { display: false }, border: { display: false } }
      },
      plugins: { legend: { display: false } }
    };
  }

  carregarGraficosSecundarios(): void {
    const filtrosApi = { dataInicio: this.dataInicio, dataFim: this.dataFim };
    const apiCall = this.analysisType === 'funcionarios' 
      ? this.apiService.getDesempenhoFuncionarios(filtrosApi) 
      : this.apiService.getDesempenhoMaquinas(filtrosApi);

    apiCall.subscribe({
      next: (data) => {
        if (data && data.length > 0) {
          const labels = data.map((i: any) => i.nomeFuncionario || i.nomeMaquina);
          const values = data.map((i: any) => i.totalHoras);
          this.doughnutChartData = {
            labels,
            datasets: [{ 
              data: values, 
              backgroundColor: ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6', '#ef4444', '#06b6d4'],
              borderWidth: 0
            }]
          };
        } else {
          this.doughnutChartData = undefined;
        }
        this.isLoading = false;
      },
      error: () => this.isLoading = false
    });
  }

  private gerarArrayDeDatas(inicio: string, fim: string): string[] {
    const lista: string[] = [];
    const dAtual = new Date(inicio + 'T12:00:00');
    const dFim = new Date(fim + 'T12:00:00');
    while (dAtual <= dFim && lista.length < 365) {
      lista.push(dAtual.toISOString().split('T')[0]);
      dAtual.setDate(dAtual.getDate() + 1);
    }
    return lista;
  }

  setAnalysisType(type: 'funcionarios' | 'maquinas'): void {
    if (this.analysisType !== type) {
      this.analysisType = type;
      this.carregarGraficosSecundarios();
    }
  }

  exportarPdf(): void {
    this.isExportingPdf = true;
    this.apiService.gerarRelatorioPdf({ dataInicio: this.dataInicio, dataFim: this.dataFim }).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a'); a.href = url;
        a.download = `relatorio_producao_${new Date().toISOString().slice(0, 10)}.pdf`;
        document.body.appendChild(a); a.click(); window.URL.revokeObjectURL(url); a.remove();
        this.isExportingPdf = false;
      },
      error: () => { this.toastService.error('Erro ao gerar PDF'); this.isExportingPdf = false; }
    });
  }

  // --- LÓGICA DE MODAIS E CRUD ---
  openEditModal(reg: any): void {
    this.currentRegistro = { ...reg };
    try { this.currentRegistro.data = this.datePipe.transform(reg.data, 'yyyy-MM-dd'); } catch { this.currentRegistro.data = null; }
    this.currentRegistro.operador = { id: reg.operador?.id };
    this.isEditModalVisible = true;
  }

  closeEditModal(): void { this.isEditModalVisible = false; this.currentRegistro = null; }

  saveChanges(): void {
    if (!this.currentRegistro) return;
    const dataToUpdate = {
      id: this.currentRegistro.id,
      data: this.currentRegistro.data,
      horimetroInicial: this.currentRegistro.horimetroInicial,
      horimetroFinal: this.currentRegistro.horimetroFinal,
      abastecimento: this.currentRegistro.abastecimento,
      servico: this.currentRegistro.servico,
      cliente: this.currentRegistro.cliente,
      descricao: this.currentRegistro.descricao,
      latitude: this.currentRegistro.latitude,
      longitude: this.currentRegistro.longitude,
      operador: { id: this.currentRegistro.operador.id }
    };

    this.apiService.updateRegistro(this.currentRegistro.id, dataToUpdate).subscribe({
      next: () => {
        this.loadData();
        this.toastService.success('Registro atualizado com sucesso!');
        this.closeEditModal();
      },
      error: (err) => {
        this.toastService.error('Erro ao salvar as alterações.');
      }
    });
  }
  
  deleteRegistro(id: number): void {
    this.confirmService.open('Deseja excluir permanentemente este lançamento?', 'Excluir').subscribe(c => {
      if (c) this.apiService.deleteRegistro(id).subscribe({
        next: () => { this.loadData(); this.toastService.success('Lançamento excluído!'); },
        error: (err) => this.toastService.error(`Erro: ${err.message}`)
      });
    });
  }
  
  countFotos(reg: any): number { return (reg.caminhoAnexoInicio ? 1 : 0) + (reg.caminhoAnexoFinal ? 1 : 0) + (reg.caminhoAnexo ? 1 : 0); }

  // =========================================================================
  // NOVO SISTEMA INTELIGENTE DA GALERIA (LAZY LOADING E ACESSO DIRETO A URL)
  // =========================================================================

  openGaleriaModal(reg: any) {
    this.isAnexoModalVisible = true;
    this.currentFotoRegistro = reg;
    this.anexoInicioUrl = null; 
    this.anexoFinalUrl = null; 
    this.anexoUnicaUrl = null;
    this.isLoadingAnexoInicio = false;
    this.isLoadingAnexoFinal = false;

    // Define qual será a aba inicial a ser mostrada baseada no que existe no registro
    if (reg.caminhoAnexoInicio) this.activePhotoTab = 'inicio';
    else if (reg.caminhoAnexoFinal) this.activePhotoTab = 'final';
    else if (reg.caminhoAnexo) this.activePhotoTab = 'unica';

    // Dispara o carregamento inteligente APENAS da foto que o usuário vai ver agora
    this.loadActivePhoto();
  }

  switchPhotoTab(tab: 'inicio' | 'final' | 'unica') {
    this.activePhotoTab = tab;
    this.loadActivePhoto();
  }

  loadActivePhoto() {
    const reg = this.currentFotoRegistro;
    if (!reg) return;

    // ATENÇÃO: A URL base do seu servidor Java onde configuramos o WebConfig.java
    const baseUrl = 'https://realizzacontrole.kodarsoftwares.com.br/api/files/anexos/';
    //const baseUrl = 'http://localhost:8082/api/files/anexos/';

    // Ao invés de baixar via TypeScript (que trava a memória), passamos a URL direto pro HTML!
    if (this.activePhotoTab === 'inicio' && reg.caminhoAnexoInicio && !this.anexoInicioUrl) {
      this.isLoadingAnexoInicio = true; // Ativa o spinner animado
      this.anexoInicioUrl = this.sanitizer.bypassSecurityTrustUrl(baseUrl + reg.caminhoAnexoInicio);
    } 
    else if (this.activePhotoTab === 'final' && reg.caminhoAnexoFinal && !this.anexoFinalUrl) {
      this.isLoadingAnexoFinal = true;
      this.anexoFinalUrl = this.sanitizer.bypassSecurityTrustUrl(baseUrl + reg.caminhoAnexoFinal);
    } 
    else if (this.activePhotoTab === 'unica' && reg.caminhoAnexo && !this.anexoUnicaUrl) {
      this.isLoadingAnexoInicio = true; 
      this.anexoUnicaUrl = this.sanitizer.bypassSecurityTrustUrl(baseUrl + reg.caminhoAnexo);
    }
  }

  // Desliga a animação de loading assim que o navegador terminar de baixar a imagem
  onImageLoad(type: 'inicio' | 'final' | 'unica') {
    if (type === 'inicio' || type === 'unica') {
      this.isLoadingAnexoInicio = false;
    } else if (type === 'final') {
      this.isLoadingAnexoFinal = false;
    }
  }

  closeAnexoModal(): void { 
    this.isAnexoModalVisible = false; 
    this.currentFotoRegistro = null;
  }
  
  // =========================================================================

  openMapModal(lat: number, lng: number): void {
    if (lat && lng) {
      const url = `https://maps.google.com/maps?q=${lat},${lng}&hl=pt-BR&z=15&output=embed`;
      this.mapUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
      this.isMapModalVisible = true;
    } else {
      this.toastService.info('Localização GPS não disponível para este registro.');
    }
  }
  closeMapModal(): void { this.isMapModalVisible = false; this.mapUrl = null; }
}