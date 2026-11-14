import { Component, ElementRef, ViewChild, inject } from '@angular/core';
import { TrackerStore } from '../tracker.store';

@Component({
  selector: 'app-delete-dialog',
  template: `
    <dialog #dialog aria-labelledby="delete-title" (cancel)="cancel($event)">
      <h2 id="delete-title">Confirmar exclusão</h2>
      <p>Deseja excluir <strong>{{ label }}</strong>?</p>
      <p class="muted">O registro sairá das listas. Os dados e o histórico serão preservados.</p>
      @if (store.error()) { <p class="error" role="alert">{{ store.error() }}</p> }
      <div class="actions modal-actions">
        <button (click)="dialog.close()" [disabled]="store.busy()">Cancelar</button>
        <button class="danger" (click)="confirm()" [disabled]="store.busy()">{{ store.busy() ? 'Excluindo…' : 'Excluir' }}</button>
      </div>
    </dialog>
  `
})
export class DeleteDialog {
  readonly store = inject(TrackerStore);
  @ViewChild('dialog', { static: true }) dialog!: ElementRef<HTMLDialogElement>;
  label = '';
  private path = '';
  open(path: string, label: string) {
    this.path = path;
    this.label = label;
    this.store.error.set('');
    this.dialog.nativeElement.showModal();
  }
  cancel(event: Event) { if (this.store.busy()) event.preventDefault(); }
  confirm() {
    void this.store.run(async () => {
      await this.store.api.delete(this.path);
      this.dialog.nativeElement.close();
      await this.store.load();
    });
  }
}
