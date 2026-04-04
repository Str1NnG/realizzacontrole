import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent {
  @Input() isExpanded = true;
  @Output() toggle = new EventEmitter<void>();

  constructor(private authService: AuthService) {}

  onToggle() {
    this.toggle.emit();
  }

  logout() {
    this.authService.logout();
  }
}