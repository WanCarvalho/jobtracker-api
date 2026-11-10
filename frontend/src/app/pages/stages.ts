import { Component, ElementRef, ViewChild, inject } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TrackerStore } from '../tracker.store';

@Component({
  imports: [FormsModule, RouterLink],
  template: `
    <div class="heading"><div><p class="eyebrow">ETAPAS</p><h1>Etapas</h1></div><div class="actions"><a routerLink="/">Voltar às candidaturas</a><button class="primary" (click)="open()" [disabled]="store.busy()">Cadastrar etapa</button></div></div>
    <section class="list-panel" aria-labelledby="stages-heading" [attr.aria-busy]="store.busy()">
      <div class="list-heading"><div><h2 id="stages-heading">Minhas etapas <span class="count-badge">{{ store.stages().length }}</span></h2><p>As etapas definem o caminho das suas candidaturas, do início à conclusão.</p></div><span class="list-hint">Ordem do processo</span></div>
      <table class="data-table">
        <caption class="sr-only">Etapas na ordem do processo e tipo de cada etapa</caption>
        <thead><tr><th scope="col" class="order-column">Ordem</th><th scope="col">Etapa</th><th scope="col">Tipo</th></tr></thead>
        <tbody>
          @for (stage of store.stages(); track stage.id; let index = $index) {
            <tr><td data-label="Ordem"><span class="order-number">{{ (index + 1).toString().padStart(2, '0') }}</span></td>
              <td data-label="Etapa"><div class="item-identity"><span class="color-swatch" aria-hidden="true" [style.background-color]="stage.color"></span><span class="item-title">{{ stage.name }}</span></div></td>
              <td data-label="Tipo"><span class="status-badge" [class.status-final]="stage.isFinal" [class.status-progress]="!stage.isFinal"><span class="status-dot" aria-hidden="true"></span>{{ stage.isFinal ? 'Etapa final' : 'Em andamento' }}</span></td></tr>
          } @empty {
            <tr><td colspan="3" class="empty-cell"><div class="empty-state"><span class="empty-symbol" aria-hidden="true">≡</span><h3>{{ store.busy() ? 'Carregando etapas…' : 'Organize seu processo seletivo' }}</h3><p>{{ store.busy() ? 'Aguarde enquanto buscamos seus dados.' : 'Crie sua primeira etapa para acompanhar o progresso das candidaturas.' }}</p>@if (!store.busy()) { <button (click)="open()">Cadastrar etapa</button> }</div></td></tr>
          }
        </tbody>
      </table>
    </section>
    <dialog #stageDialog aria-labelledby="stage-title" (cancel)="cancel($event)">
      <div class="heading"><h2 id="stage-title">Nova etapa</h2><button type="button" aria-label="Fechar cadastro de etapa" (click)="close()" [disabled]="store.busy()">✕</button></div>
      @if (store.error()) { <p class="error" role="alert">{{ store.error() }}</p> }
      <form #form="ngForm" (ngSubmit)="create()">
        <label>Nome da etapa<input name="name" [(ngModel)]="stage.name" required maxlength="100" autofocus></label>
        <div class="inline"><label>Cor<input type="color" name="color" [(ngModel)]="stage.color"></label><label class="check"><input type="checkbox" name="isFinal" [(ngModel)]="stage.isFinal">Etapa final</label></div>
        <div class="actions modal-actions"><button type="button" (click)="close()" [disabled]="store.busy()">Cancelar</button><button class="primary" [disabled]="form.invalid || store.busy()">{{ store.busy() ? 'Salvando…' : 'Salvar etapa' }}</button></div>
      </form>
    </dialog>
  `
})
export class Stages {
  readonly store = inject(TrackerStore);
  @ViewChild('stageDialog', { static: true }) dialog!: ElementRef<HTMLDialogElement>;
  @ViewChild('form', { static: true }) form!: NgForm;
  stage = { name: '', color: '#2563eb', isFinal: false };
  open() {
    this.store.error.set('');
    this.stage = { name: '', color: '#2563eb', isFinal: false };
    this.form.resetForm(this.stage);
    this.dialog.nativeElement.showModal();
  }
  close() { if (!this.store.busy()) this.dialog.nativeElement.close(); }
  cancel(event: Event) { if (this.store.busy()) event.preventDefault(); }
  create() {
    if (this.form.invalid) return;
    void this.store.run(async () => {
      await this.store.api.post('/stages', { ...this.stage, position: this.store.stages().length });
      this.dialog.nativeElement.close();
      await this.store.load();
    });
  }
}
