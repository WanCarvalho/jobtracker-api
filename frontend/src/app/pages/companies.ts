import { Component, ElementRef, ViewChild, inject } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { TrackerStore } from '../tracker.store';

@Component({
  imports: [FormsModule, RouterLink],
  template: `
    <div class="heading"><div><p class="eyebrow">EMPRESAS</p><h1>Empresas</h1></div><div class="actions"><a routerLink="/">Voltar às candidaturas</a><button class="primary" (click)="open()" [disabled]="store.busy()">Cadastrar empresa</button></div></div>
    <section class="list-panel" aria-labelledby="companies-heading" [attr.aria-busy]="store.busy()">
      <div class="list-heading"><div><h2 id="companies-heading">Empresas disponíveis <span class="count-badge">{{ store.companies().length }}</span></h2><p>Consulte as empresas que você pode vincular às suas candidaturas.</p></div></div>
      <table class="data-table">
        <caption class="sr-only">Empresas disponíveis e situação da aprovação</caption>
        <thead><tr><th scope="col">Empresa</th><th scope="col">Situação</th></tr></thead>
        <tbody>
          @for (company of store.companies(); track company.id) {
            <tr><td data-label="Empresa"><div class="item-identity"><span class="item-avatar" aria-hidden="true">{{ company.name.slice(0, 1).toUpperCase() }}</span><div><span class="item-title">{{ company.name }}</span><span class="item-meta">Empresa #{{ company.id }}</span></div></div></td>
              <td data-label="Situação"><span class="status-badge" [class.status-approved]="company.approvalStatus === 'APPROVED'" [class.status-pending]="company.approvalStatus === 'PENDING'" [class.status-rejected]="company.approvalStatus === 'REJECTED'"><span class="status-dot" aria-hidden="true"></span>{{ status(company.approvalStatus) }}</span></td></tr>
          } @empty {
            <tr><td colspan="2" class="empty-cell"><div class="empty-state"><span class="empty-symbol" aria-hidden="true">▦</span><h3>{{ store.busy() ? 'Carregando empresas…' : 'Nenhuma empresa por aqui ainda' }}</h3><p>{{ store.busy() ? 'Aguarde enquanto buscamos seus dados.' : 'Cadastre uma empresa para começar a organizar suas oportunidades.' }}</p>@if (!store.busy()) { <button (click)="open()">Cadastrar empresa</button> }</div></td></tr>
          }
        </tbody>
      </table>
    </section>
    <dialog #companyDialog aria-labelledby="company-title" (cancel)="cancel($event)">
      <div class="heading"><h2 id="company-title">Nova empresa</h2><button type="button" aria-label="Fechar cadastro de empresa" (click)="close()" [disabled]="store.busy()">✕</button></div>
      @if (store.error()) { <p class="error" role="alert">{{ store.error() }}</p> }
      <form #form="ngForm" (ngSubmit)="create()">
        <label>Nome da empresa<input name="name" [(ngModel)]="company.name" required maxlength="180" autofocus></label>
        <p class="muted">Empresas novas aguardam aprovação, mas você já pode usá-las.</p>
        <div class="actions modal-actions"><button type="button" (click)="close()" [disabled]="store.busy()">Cancelar</button><button class="primary" [disabled]="form.invalid || store.busy()">{{ store.busy() ? 'Salvando…' : 'Salvar empresa' }}</button></div>
      </form>
    </dialog>
  `
})
export class Companies {
  readonly store = inject(TrackerStore);
  @ViewChild('companyDialog', { static: true }) dialog!: ElementRef<HTMLDialogElement>;
  @ViewChild('form', { static: true }) form!: NgForm;
  company = { name: '' };
  open() {
    this.store.error.set('');
    this.company = { name: '' };
    this.form.resetForm(this.company);
    this.dialog.nativeElement.showModal();
  }
  close() { if (!this.store.busy()) this.dialog.nativeElement.close(); }
  cancel(event: Event) { if (this.store.busy()) event.preventDefault(); }
  create() {
    if (this.form.invalid) return;
    void this.store.run(async () => {
      await this.store.api.post('/companies', this.company);
      this.dialog.nativeElement.close();
      await this.store.load();
    });
  }
  status(value: string) { return ({ PENDING: 'Aguardando aprovação', APPROVED: 'Aprovada', REJECTED: 'Rejeitada' } as Record<string, string>)[value] ?? value; }
}
