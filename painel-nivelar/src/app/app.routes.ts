import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { SettingsComponent } from './components/settings/settings.component';
import { ManagementComponent } from './components/management/management.component';
import { authGuard } from './guards/auth.guard';
import { AdminLayoutComponent } from './layouts/admin/admin.component';
import { FinanceiroComponent } from './components/financeiro/financeiro.component';
import { ClientesComponent } from './components/cliente/cliente.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: AdminLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'management', component: ManagementComponent },
      { path: 'settings', component: SettingsComponent },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'financeiro', component: FinanceiroComponent },
      { path: 'clientes', component: ClientesComponent},
    ]
  },
  { path: '**', redirectTo: '' }
];