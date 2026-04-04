import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './modal.component.html',
})
export class ModalComponent {

  @Input() visible: boolean = false;
  @Output() visibleChange: EventEmitter<boolean> = new EventEmitter<boolean>();

  @Input() maxWidth: string = '500px'; 
  @Input() contentClass: string = '';

  constructor() { }

  public closeModal(): void {
    this.visible = false;
    this.visibleChange.emit(this.visible);
  }

  public onOverlayClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.closeModal();
    }
  }

  public onContentClick(event: MouseEvent): void {
    event.stopPropagation();
  }
}