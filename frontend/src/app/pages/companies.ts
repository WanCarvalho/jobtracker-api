import { Component, ElementRef, ViewChild, inject } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Company } from '../api';
import { DeleteDialog } from '../shared/delete-dialog';
import { TrackerStore } from '../tracker.store';

@Component({
  imports: [FormsModule, RouterLink, DeleteDialog],
  template: `
    <div class="heading"><div><p class="eyebrow">EMPRESAS</p><h1>Empresas</h1></div><div class="actions"><a routerLink="/">Voltar às candidaturas</a><button class="primary" (click)="open()" [disabled]="store.busy()">Cadastrar empresa</button></div></div>
    <section class="list-panel" aria-labelledby="companies-heading" [attr.aria-busy]="store.busy()">
      <div class="list-heading"><div><h2 id="companies-heading">Empresas disponíveis <span class="count-badge">{{ store.companies().length }}</span></h2><p>Consulte as empresas que você pode vincular às suas candidaturas.</p></div></div>
      <table class="data-table">
        <caption class="sr-only">Empresas disponíveis e situação da aprovação</caption>
        <thead><tr><th scope="col">Empresa</th><th scope="col">Situação</th><th scope="col">Ações</th></tr></thead>
        <tbody>
          @for (company of store.companies(); track company.id) {
            <tr><td data-label="Empresa"><div class="item-identity"><span class="item-avatar" aria-hidden="true">{{ company.name.slice(0, 1).toUpperCase() }}</span><div><span class="item-title">{{ company.name }}</span><span class="item-meta">Empresa #{{ company.id }}</span></div></div></td>
              <td data-label="Situação"><span class="status-badge" [class.status-approved]="company.approvalStatus === 'APPROVED'" [class.status-pending]="company.approvalStatus === 'PENDING'" [class.status-rejected]="company.approvalStatus === 'REJECTED'"><span class="status-dot" aria-hidden="true"></span>{{ status(company.approvalStatus) }}</span></td><td data-label="Ações"><div class="actions row-actions">@if (websiteUrl(company.website); as website) { <a class="button-link" [href]="website" target="_blank" rel="noopener noreferrer" [attr.aria-label]="'Acessar site de ' + company.name + ' (abre em nova aba)'">Acessar site</a> }@if (canManage(company)) {<button (click)="open(company)" [disabled]="store.busy()" [attr.aria-label]="'Editar ' + company.name">Editar</button><button class="danger" (click)="deletion.open('/companies/' + company.id, company.name)" [disabled]="store.busy()" [attr.aria-label]="'Excluir ' + company.name">Excluir</button>}</div></td></tr>
          } @empty {
            <tr><td colspan="3" class="empty-cell"><div class="empty-state"><span class="empty-symbol" aria-hidden="true">▦</span><h3>{{ store.busy() ? 'Carregando empresas…' : 'Nenhuma empresa por aqui ainda' }}</h3><p>{{ store.busy() ? 'Aguarde enquanto buscamos seus dados.' : 'Cadastre uma empresa para começar a organizar suas oportunidades.' }}</p>@if (!store.busy()) { <button (click)="open()">Cadastrar empresa</button> }</div></td></tr>
          }
        </tbody>
      </table>
    </section>
    <app-delete-dialog #deletion />
    <dialog #companyDialog aria-labelledby="company-title" (cancel)="cancel($event)">
      <div class="heading"><h2 id="company-title">{{ editingId ? 'Editar empresa' : 'Nova empresa' }}</h2><button type="button" aria-label="Fechar cadastro de empresa" (click)="close()" [disabled]="store.busy()">✕</button></div>
      @if (store.error()) { <p class="error" role="alert">{{ store.error() }}</p> }
      <form #form="ngForm" (ngSubmit)="save()"><fieldset [disabled]="store.busy()">
        <label>Nome da empresa<input name="name" [(ngModel)]="company.name" required maxlength="180" autofocus></label>
        <label>CNPJ<input name="cnpj" [(ngModel)]="company.cnpj" maxlength="18"></label>
        <label>Site<input name="website" [(ngModel)]="company.website" maxlength="255"></label>
        <label>Descrição<textarea name="description" [(ngModel)]="company.description" rows="4"></textarea></label>
        @if (selected) { <p>Situação: {{ status(selected.approvalStatus) }}</p><p>Cadastrada por: {{ selected.createdByName }}</p>@if (selected.reviewNotes) { <p>Observações da análise: {{ selected.reviewNotes }}</p> } }
        <p class="muted">Empresas novas aguardam aprovação, mas você já pode usá-las.</p>
        </fieldset><div class="actions modal-actions"><button type="button" (click)="close()" [disabled]="store.busy()">Cancelar</button><button class="primary" [disabled]="form.invalid || store.busy()">{{ store.busy() ? 'Salvando…' : 'Salvar empresa' }}</button></div>
      </form>
    </dialog>
  `
})
export class Companies {
  readonly store = inject(TrackerStore);
  @ViewChild('companyDialog', { static: true }) dialog!: ElementRef<HTMLDialogElement>;
  @ViewChild('form', { static: true }) form!: NgForm;
  company = { name: '', cnpj: '', website: '', description: '' };
  selected: Company | null = null;
  editingId: number | null = null;
  open(record?: Company) {
    this.store.error.set('');
    this.editingId = record?.id ?? null;
    this.selected = record ?? null;
    this.company = { name: record?.name ?? '', cnpj: record?.cnpj ?? '', website: record?.website ?? '', description: record?.description ?? '' };
    this.form.resetForm(this.company);
    this.dialog.nativeElement.showModal();
  }
  close() { if (!this.store.busy()) this.dialog.nativeElement.close(); }
  cancel(event: Event) { if (this.store.busy()) event.preventDefault(); }
  save() {
    if (this.form.invalid) return;
    void this.store.run(async () => {
      if (this.editingId) await this.store.api.put('/companies/' + this.editingId, this.company);
      else await this.store.api.post('/companies', this.company);
      this.dialog.nativeElement.close();
      await this.store.load();
    });
  }
  websiteUrl(website: string | null): string | null {
    const value = website?.trim();
    if (!value) return null;
    try {
      const url = new URL(value.startsWith('//') ? 'https:' + value
        : /^[a-z][a-z\d+.-]*:/i.test(value) ? value : 'https://' + value);
      return url.protocol === 'https:' || url.protocol === 'http:' ? url.href : null;
    } catch { return null; }
  }
  canManage(company: Company) { return company.createdById === this.store.api.session()?.userId && company.approvalStatus !== 'APPROVED'; }
  status(value: string) { return ({ PENDING: 'Aguardando aprovação', APPROVED: 'Aprovada', REJECTED: 'Rejeitada' } as Record<string, string>)[value] ?? value; }
}
