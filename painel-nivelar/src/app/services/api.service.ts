import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // --- REGISTROS / HISTÓRICO ---
  
  getAllRegistros(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/registros`)
      .pipe(catchError(this.handleError));
  }

  // NOVO MÉTODO: Chamado pelo ClientesComponent para buscar o histórico de localizações
  getHistoricoServicos(): Observable<any[]> {
    // Reutilizando sua rota de registros para buscar os dados de geolocalização
    return this.http.get<any[]>(`${this.apiUrl}/registros`)
      .pipe(catchError(this.handleError));
  }

  updateRegistro(id: number, data: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/registros/${id}`, data)
      .pipe(catchError(this.handleError));
  }

  deleteRegistro(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/registros/${id}`)
      .pipe(catchError(this.handleError));
  }

  // --- ANEXOS ---
  getAnexoBytes(id: number, tipo: 'inicio' | 'final' | 'principal' = 'principal'): Observable<Blob> {
    let endpoint = `${this.apiUrl}/registros/${id}/anexo`;
    if (tipo === 'inicio') endpoint = `${this.apiUrl}/registros/${id}/anexo-inicio`;
    if (tipo === 'final') endpoint = `${this.apiUrl}/registros/${id}/anexo-final`;
    
    return this.http.get(endpoint, { responseType: 'blob' })
      .pipe(catchError(this.handleError));
  }

  // --- OPERADORES / PERFIL ---
  updateProfile(updates: { nomeEmpresa?: string }): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/operadores/profile`, updates)
      .pipe(catchError(this.handleError));
  }

  uploadFotoPerfil(file: File): Observable<{ fotoPerfilUrl: string }> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<{ fotoPerfilUrl: string }>(`${this.apiUrl}/operadores/profile-picture`, formData)
      .pipe(catchError(this.handleError));
  }

  // --- GESTÃO DE OPERADORES ---
  getAllOperadores(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/admin/operadores/all`)
      .pipe(catchError(this.handleError));
  }

  createOperador(data: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/admin/operadores`, data)
      .pipe(catchError(this.handleError));
  }

  updateOperador(id: number, data: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/admin/operadores/${id}`, data)
      .pipe(catchError(this.handleError));
  }

  deleteOperador(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/admin/operadores/${id}`)
      .pipe(catchError(this.handleError));
  }

  // --- MÁQUINAS ---
  getAllMaquinas(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/admin/maquinas/all`)
      .pipe(catchError(this.handleError));
  }

  createMaquina(maquinaData: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/admin/maquinas`, maquinaData)
      .pipe(catchError(this.handleError));
  }

  updateMaquina(id: number, maquinaData: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/admin/maquinas/${id}`, maquinaData)
      .pipe(catchError(this.handleError));
  }

  deleteMaquina(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/admin/maquinas/${id}`)
      .pipe(catchError(this.handleError));
  }

  // --- RELATÓRIOS ---
  getDesempenhoFuncionarios(filtros: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/reports/funcionarios`, filtros)
      .pipe(catchError(this.handleError));
  }

  getDesempenhoMaquinas(filtros: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/reports/maquinas`, filtros)
      .pipe(catchError(this.handleError));
  }

  gerarRelatorioPdf(filtros: any): Observable<Blob> {
    return this.http.post(`${this.apiUrl}/reports/pdf`, filtros, { responseType: 'blob' })
      .pipe(catchError(this.handleError));
  }

  // --- FINANCEIRO ---
  marcarComoPago(ids: number[]): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/registros/pagar`, ids)
      .pipe(catchError(this.handleError));
  }

  // --- ROTAS DE CLIENTES ---
  getClientes(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/clientes`)
      .pipe(catchError(this.handleError));
  }

  createCliente(cliente: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/clientes`, cliente)
      .pipe(catchError(this.handleError));
  }

  updateCliente(id: number, cliente: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/clientes/${id}`, cliente)
      .pipe(catchError(this.handleError));
  }

  deleteCliente(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/clientes/${id}`)
      .pipe(catchError(this.handleError));
  }

  getRegistrosFinanceiros(): Observable<any[]> {
  return this.http.get<any[]>(`${this.apiUrl}/registros/financeiro`)
    .pipe(catchError(this.handleError));
}

  createRegistroManual(dados: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/registros/manual`, dados);
  }

  getOperadores(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/operadores`);
  }

  // --- Tratamento de Erros ---
  private handleError(error: HttpErrorResponse): Observable<never> {
    console.error('Erro na API:', error);
    const errorMessage = error.error?.message || 'Ocorreu um erro desconhecido.';
    return throwError(() => new Error(errorMessage));
  }
}