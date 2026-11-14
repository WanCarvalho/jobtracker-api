import { Injectable, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Api, Application, Company, Stage, Summary } from './api';

@Injectable({ providedIn: 'root' })
export class TrackerStore {
  readonly api = inject(Api);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly applications = signal<Application[]>([]);
  readonly companies = signal<Company[]>([]);
  readonly stages = signal<Stage[]>([]);
  readonly summary = signal<Summary | null>(null);
  register = false;
  credentials = { name: '', email: '', password: '' };

  constructor() {
    if (this.api.session()) void this.run(() => this.load());
  }
  async run(action: () => Promise<unknown>) {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    try {
      await action();
    } catch (error) {
      this.reportError(error);
    } finally {
      this.busy.set(false);
    }
  }
  reportError(error: unknown) {
    const e = error as HttpErrorResponse;
    this.error.set(
      e.status === 0
        ? 'Não foi possível conectar à API. Verifique se o backend está em execução.'
        : e.status === 401
          ? 'Sessão expirada ou credenciais inválidas. Entre novamente.'
          : e.error?.error ||
            (e.error?.errors
              ? Object.values(e.error.errors).join('; ')
              : 'Não foi possível concluir a operação.'),
    );
  }
  async load() {
    const [applications, companies, stages, summary] = await Promise.all([
      this.api.get<Application[]>('/applications'),
      this.api.get<Company[]>('/companies'),
      this.api.get<Stage[]>('/stages'),
      this.api.get<Summary>('/dashboard/summary'),
    ]);
    this.applications.set(applications);
    this.companies.set(companies);
    this.stages.set(stages.filter((s) => s.isActive));
    this.summary.set(summary);
  }
  login() {
    void this.run(async () => {
      await this.api.authenticate(this.register, this.credentials);
      this.credentials.password = '';
      await this.load();
    });
  }
  logout() {
    this.api.logout();
    this.applications.set([]);
    this.companies.set([]);
    this.stages.set([]);
    this.summary.set(null);
    this.error.set('');
  }
  refresh() {
    void this.run(() => this.load());
  }
  move(id: number, stageId: string) {
    void this.run(async () => {
      await this.api.patch(`/applications/${id}/stage`, { stageId: Number(stageId) });
      await this.load();
    });
  }
}
