import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Api, Company } from '../api';

@Component({
  imports: [FormsModule],
  template: `
    <div class="heading">
      <div>
        <p class="eyebrow">ADMINISTRAÇÃO</p>
        <h1>Moderação de empresas</h1>
      </div>
      <button (click)="load()" [disabled]="loading() || reviewingId() !== null">
        {{ loading() ? 'Atualizando…' : 'Atualizar' }}
      </button>
    </div>
    @if (notice()) {
      <p class="notice" role="status" aria-live="polite">{{ notice() }}</p>
    }
    @if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    }
    <section aria-labelledby="pending-heading" [attr.aria-busy]="loading()">
      <div class="section-heading">
        <div>
          <h2 id="pending-heading">Cadastros pendentes</h2>
          <p class="muted">Confira os dados e possíveis equivalentes antes de analisar.</p>
        </div>
        <span class="count-badge">{{ pending().length }}</span>
      </div>
      @if (loading() && pending().length === 0) {
        <p role="status">Carregando cadastros…</p>
      } @else if (pending().length === 0) {
        <p class="empty-state">Não há cadastros pendentes para analisar.</p>
      } @else {
        <div class="review-list">
          @for (company of pending(); track company.id) {
            <article class="review-card">
              <div class="company-heading">
                <div>
                  <p class="eyebrow">CADASTRO #{{ company.id }}</p>
                  <h3>{{ company.name }}</h3>
                </div>
                <span class="status-badge status-pending">Aguardando aprovação</span>
              </div>
              <dl class="company-details">
                <dt>Cadastrada por</dt>
                <dd>{{ company.createdByName }}</dd>
                <dt>CNPJ</dt>
                <dd>{{ company.cnpj || 'Não informado' }}</dd>
                <dt>Site</dt>
                <dd>
                  @if (company.website && websiteUrl(company.website); as website) {
                    <a [href]="website" target="_blank" rel="noopener noreferrer">
                      {{ company.website }}
                    </a>
                  } @else {
                    Não informado
                  }
                </dd>
                <dt>Descrição</dt>
                <dd>{{ company.description || 'Não informada' }}</dd>
              </dl>
              <div class="equivalents">
                <h4>Possíveis equivalentes</h4>
                @if (equivalentsFor(company).length) {
                  @for (equivalent of equivalentsFor(company); track equivalent.id) {
                    <div class="equivalent">
                      <span>
                        <strong>{{ equivalent.name }}</strong>
                        <span class="muted">
                          · {{ equivalent.cnpj || 'CNPJ não informado' }} ·
                          {{ equivalent.createdByName }}
                        </span>
                      </span>
                      <span
                        class="status-badge"
                        [class.status-approved]="equivalent.approvalStatus === 'APPROVED'"
                        [class.status-pending]="equivalent.approvalStatus === 'PENDING'"
                        [class.status-rejected]="equivalent.approvalStatus === 'REJECTED'"
                      >
                        {{ statusLabel(equivalent.approvalStatus) }}
                      </span>
                    </div>
                  }
                } @else {
                  <p class="muted">Nenhum equivalente encontrado pelo nome ou CNPJ.</p>
                }
              </div>
              @if (rejectingId() === company.id) {
                <label>
                  Motivo da rejeição
                  <textarea
                    [(ngModel)]="rejectionReason"
                    name="rejectionReason"
                    rows="3"
                    maxlength="1000"
                    required
                    aria-label="Motivo da rejeição de {{ company.name }}"
                  ></textarea>
                </label>
              }
              <div class="actions review-actions">
                @if (rejectingId() === company.id) {
                  <button
                    type="button"
                    (click)="cancelReject()"
                    [disabled]="reviewingId() !== null"
                  >
                    Cancelar
                  </button>
                  <button
                    type="button"
                    class="danger"
                    (click)="reject(company)"
                    [disabled]="
                      reviewingId() !== null ||
                      !rejectionReason.trim() ||
                      rejectionReason.length > 1000
                    "
                  >
                    {{ reviewingId() === company.id ? 'Rejeitando…' : 'Confirmar rejeição' }}
                  </button>
                } @else {
                  <button
                    type="button"
                    class="danger"
                    (click)="startReject(company.id)"
                    [disabled]="reviewingId() !== null"
                  >
                    Rejeitar
                  </button>
                  <button
                    type="button"
                    class="primary"
                    (click)="approve(company)"
                    [disabled]="reviewingId() !== null"
                  >
                    {{ reviewingId() === company.id ? 'Aprovando…' : 'Aprovar' }}
                  </button>
                }
              </div>
            </article>
          }
        </div>
      }
    </section>
    <section aria-labelledby="reviewed-heading">
      <h2 id="reviewed-heading">Cadastros já analisados</h2>
      @if (reviewed().length) {
        <ul class="reviewed-list">
          @for (company of reviewed(); track company.id) {
            <li>
              <span>
                <strong>{{ company.name }}</strong>
                <span class="muted">· {{ company.reviewNotes || 'Sem observações.' }}</span>
              </span>
              <span
                class="status-badge"
                [class.status-approved]="company.approvalStatus === 'APPROVED'"
                [class.status-rejected]="company.approvalStatus === 'REJECTED'"
              >
                {{ statusLabel(company.approvalStatus) }}
              </span>
            </li>
          }
        </ul>
      } @else {
        <p class="muted">Ainda não há cadastros analisados.</p>
      }
    </section>
  `,
  styleUrl: './admin-companies.scss',
})
export class AdminCompanies {
  private readonly api = inject(Api);
  readonly pending = signal<Company[]>([]);
  readonly allCompanies = signal<Company[]>([]);
  readonly loading = signal(false);
  readonly reviewingId = signal<number | null>(null);
  readonly rejectingId = signal<number | null>(null);
  rejectionReason = '';
  readonly error = signal('');
  readonly notice = signal('');
  readonly reviewed = computed(() =>
    this.allCompanies().filter((company) => company.approvalStatus !== 'PENDING'),
  );

  constructor() {
    void this.load();
  }

  async load() {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set('');
    try {
      const [pending, allCompanies] = await Promise.all([
        this.api.get<Company[]>('/admin/companies/pending'),
        this.api.get<Company[]>('/admin/companies'),
      ]);
      this.pending.set(pending);
      this.allCompanies.set(allCompanies);
    } catch (error) {
      this.showError(error);
    } finally {
      this.loading.set(false);
    }
  }

  startReject(id: number) {
    this.error.set('');
    this.notice.set('');
    this.rejectingId.set(id);
    this.rejectionReason = '';
  }

  cancelReject() {
    this.rejectingId.set(null);
    this.rejectionReason = '';
  }

  async approve(company: Company) {
    await this.review(company, 'approve');
  }

  async reject(company: Company) {
    const reason = this.rejectionReason.trim();
    if (!reason || reason.length > 1000) return;
    await this.review(company, 'reject', reason);
  }

  equivalentsFor(company: Company) {
    const normalizedName = this.normalize(company.name);
    const normalizedCnpj = this.normalize(company.cnpj ?? '');
    return this.allCompanies().filter(
      (candidate) =>
        candidate.id !== company.id &&
        (this.normalize(candidate.name) === normalizedName ||
          (!!normalizedCnpj && this.normalize(candidate.cnpj ?? '') === normalizedCnpj)),
    );
  }

  websiteUrl(website: string) {
    try {
      const url = new URL(/^[a-z][a-z\d+.-]*:/i.test(website) ? website : `https://${website}`);
      return url.protocol === 'https:' || url.protocol === 'http:' ? url.href : null;
    } catch {
      return null;
    }
  }

  statusLabel(status: string) {
    return (
      {
        APPROVED: 'Aprovada',
        PENDING: 'Pendente',
        REJECTED: 'Rejeitada',
      }[status] ?? status
    );
  }

  private async review(company: Company, action: 'approve' | 'reject', reason?: string) {
    if (this.reviewingId() !== null) return;
    this.reviewingId.set(company.id);
    this.error.set('');
    this.notice.set('');
    try {
      const updated = await this.api.patch<Company>(
        `/admin/companies/${company.id}/${action}`,
        action === 'reject' ? { reason } : {},
      );
      this.allCompanies.update((companies) =>
        companies.map((candidate) => (candidate.id === updated.id ? updated : candidate)),
      );
      this.pending.update((companies) =>
        companies.filter((candidate) => candidate.id !== updated.id),
      );
      this.cancelReject();
      this.notice.set(
        action === 'approve' ? `${company.name} foi aprovada.` : `${company.name} foi rejeitada.`,
      );
    } catch (error) {
      const response = error as HttpErrorResponse;
      if (response.status === 409) {
        this.notice.set(`${company.name} já foi analisada. A lista foi atualizada.`);
        this.cancelReject();
        await this.load();
      } else {
        this.showError(error);
      }
    } finally {
      this.reviewingId.set(null);
    }
  }

  private showError(error: unknown) {
    const response = error as HttpErrorResponse;
    this.error.set(
      response.status === 0
        ? 'Não foi possível conectar à API. Verifique se o backend está em execução.'
        : response.error?.error || 'Não foi possível carregar ou analisar os cadastros.',
    );
  }

  private normalize(value: string) {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLocaleLowerCase('pt-BR')
      .replace(/[^a-z0-9]/g, '');
  }
}
