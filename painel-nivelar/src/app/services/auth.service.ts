import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, BehaviorSubject, of, throwError } from 'rxjs';
import { tap, map, catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { jwtDecode } from "jwt-decode";
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = environment.apiUrl;
  private currentUserSubject: BehaviorSubject<any | null>;
  public currentUser: Observable<any | null>;

  constructor(private http: HttpClient, private router: Router) {
    // Tenta pegar o 'operador' do token ao carregar
    this.currentUserSubject = new BehaviorSubject<any | null>(this.getDecodedToken()?.operador || this.getDecodedToken());
    this.currentUser = this.currentUserSubject.asObservable();
  }

  login(cpf: string, senha?: string): Observable<boolean> {
    return this.http.post<any>(`${this.apiUrl}/operadores/login`, { cpf, senha: senha || '' })
      .pipe(
        tap(response => {
          if (response?.token) {
            localStorage.setItem('authToken', response.token);
            // Armazena o objeto 'operador' no BehaviorSubject
            this.currentUserSubject.next(response.operador);
          } else {
            this.logout();
          }
        }),
        map(response => !!response?.token),
        catchError(() => of(false))
      );
  }

  changePassword(senhaAtual: string, novaSenha: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/operadores/change-password`, { senhaAtual, novaSenha })
      .pipe(catchError(this.handleError));
  }

  logout(): void {
    localStorage.removeItem('authToken');
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem('authToken');
  }

  // --- FUNÇÃO ADICIONADA (PARA O AuthGuard) ---
  /**
   * Verifica se o usuário está logado.
   */
  public isLoggedIn(): boolean {
    return this.getToken() !== null;
  }

  // --- FUNÇÃO CORRIGIDA (PARA O AuthGuard) ---
  /**
   * Busca o "cargo" (role) do usuário logado.
   */
  public getUserRole(): string | null {
    const currentUser = this.currentUserSubject.value;
    
    // A propriedade no seu JSON de login é 'cargo'
    if (currentUser && currentUser.cargo) {
      return currentUser.cargo; // <-- CORRIGIDO
    }
    
    return null;
  }
  // --- FIM DA CORREÇÃO ---

  private getDecodedToken(): any | null {
    const token = this.getToken();
    if (token) {
      try { return jwtDecode(token); }
      catch { localStorage.removeItem('authToken'); return null; }
    }
    return null;
  }

  updateCurrentUser(userData: any): void {
    const updatedData = { ...this.currentUserSubject.value, ...userData };
    this.currentUserSubject.next(updatedData);
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    const userMessage = error.error?.message || 'Erro desconhecido';
    return throwError(() => new Error(userMessage));
  }
}