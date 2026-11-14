import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { Application } from '../api';
import { TrackerStore } from '../tracker.store';
import { Select } from '../shared/select';

@Component({
  imports: [FormsModule, RouterLink, Select],
  template: `
    <div class="heading"><div><p class="eyebrow">CANDIDATURA</p><h1>{{ readOnly() ? 'Visualizar candidatura' : 'Editar candidatura' }}</h1></div><a routerLink="/">Voltar às candidaturas</a></div>
    @if (loading()) { <p role="status">Carregando candidatura…</p> }
    @if (!loading() && application; as a) {
      <section class="detail-panel">
        <h2>{{ a.jobTitle }} · {{ a.companyName }}</h2>
        @if (readOnly()) {
          <dl class="record-details">
            <dt>Vaga</dt><dd>{{ a.jobTitle }}</dd>
            <dt>Empresa</dt><dd>{{ a.companyName }}</dd>
            <dt>Etapa</dt><dd>{{ a.stageName }}</dd>
            <dt>Data da candidatura</dt><dd>{{ a.appliedAt.split('-').reverse().join('/') }}</dd>
            <dt>Salário</dt><dd>{{ a.salary ?? 'Não informado' }}</dd>
            <dt>Link da vaga</dt><dd>{{ a.jobUrl || 'Não informado' }}</dd>
            <dt>Descrição</dt><dd class="preserve-lines">{{ a.description || 'Não informada' }}</dd>
          </dl>
          <div class="actions"><a class="button-link" [routerLink]="['/candidaturas', a.id, 'editar']">Editar candidatura</a></div>
        } @else {
          <form #form="ngForm" (ngSubmit)="save(form)">
            <fieldset [disabled]="store.busy()">
              <label>Vaga<input name="jobTitle" [(ngModel)]="a.jobTitle" required maxlength="180"></label>
              <label>Empresa<app-select name="companyId" [options]="companyOptions()" [(ngModel)]="a.companyId" ariaLabel="Empresa" [disabled]="store.busy()" [required]="true" /></label>
              <label>Etapa<app-select name="stageId" [options]="stageOptions()" [(ngModel)]="a.stageId" ariaLabel="Etapa" [disabled]="store.busy()" [required]="true" /></label>
              <label>Data da candidatura<input type="date" name="appliedAt" [(ngModel)]="a.appliedAt" required></label>
              <label>Salário<input type="number" name="salary" [(ngModel)]="a.salary" min="0" step="0.01"></label>
              <label>Link da vaga<input name="jobUrl" [(ngModel)]="a.jobUrl" maxlength="1000"></label>
              <label>Descrição<textarea name="description" [(ngModel)]="a.description" rows="6"></textarea></label>
            </fieldset>
            <div class="actions modal-actions"><a routerLink="/">Cancelar</a><button class="primary" [disabled]="form.invalid || store.busy()">{{ store.busy() ? 'Salvando…' : 'Salvar alterações' }}</button></div>
          </form>
        }
      </section>
    }
  `
})
export class ApplicationDetail {
  readonly store = inject(TrackerStore);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly params = toSignal(this.route.paramMap, { initialValue: this.route.snapshot.paramMap });
  private readonly data = toSignal(this.route.data, { initialValue: this.route.snapshot.data });
  readonly readOnly = computed(() => this.data()['readOnly'] === true);
  readonly loading = signal(true);
  private readonly original = signal<Application | null>(null);
  application: Application | null = null;
  readonly companyOptions = computed(() => {
    const options = this.store.companies().map(c => ({ value: c.id, label: c.name }));
    const current = this.original();
    if (current && !options.some(o => o.value === current.companyId)) options.push({ value: current.companyId, label: current.companyName + ' (indisponível para novas candidaturas)' });
    return options;
  });
  readonly stageOptions = computed(() => {
    const options = this.store.stages().map(s => ({ value: s.id, label: s.name }));
    const current = this.original();
    if (current && !options.some(o => o.value === current.stageId)) options.push({ value: current.stageId, label: current.stageName + ' (desativada)' });
    return options;
  });
  constructor() {
    effect((onCleanup) => {
      const id = Number(this.params().get('id'));
      const session = this.store.api.session();
      let cancelled = false;
      onCleanup(() => { cancelled = true; });
      this.loading.set(true);
      this.application = null;
      if (!session) return;
      this.store.error.set('');
      void this.store.api.get<Application>('/applications/' + id)
        .then(a => { if (!cancelled) { this.application = { ...a }; this.original.set(a); } })
        .catch(error => { if (!cancelled) this.store.reportError(error); })
        .finally(() => { if (!cancelled) this.loading.set(false); });
    });
  }
  save(form: NgForm) {
    if (this.readOnly() || form.invalid || !this.application) return;
    const a = this.application;
    void this.store.run(async () => {
      await this.store.api.put('/applications/' + a.id, {
        companyId: a.companyId, stageId: a.stageId, jobTitle: a.jobTitle,
        salary: a.salary, jobUrl: a.jobUrl, description: a.description, appliedAt: a.appliedAt
      });
      await this.store.load();
      await this.router.navigateByUrl('/');
    });
  }
}
