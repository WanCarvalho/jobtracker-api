import { Component, ElementRef, ViewChild, computed, inject } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { TrackerStore } from '../tracker.store';
import { Select } from '../shared/select';

@Component({
  imports: [FormsModule, RouterLink, DatePipe, Select],
  templateUrl: './dashboard.html'
})
export class Dashboard {
  readonly store = inject(TrackerStore);
  readonly companyOptions = computed(() => this.store.companies().map(company => ({ value: company.id, label: company.name })));
  readonly stageOptions = computed(() => this.store.stages().map(stage => ({ value: stage.id, label: stage.name })));
  move(id: number, stageId: number | null) { if (stageId !== null) this.store.move(id, String(stageId)); }
  @ViewChild('applicationDialog', { static: true }) dialog!: ElementRef<HTMLDialogElement>;
  @ViewChild('applicationForm') form?: NgForm;
  private emptyApplication() {
    const now = new Date();
    return { jobTitle: '', companyId: null as number | null, stageId: null as number | null,
      appliedAt: `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}` };
  }
  application = this.emptyApplication();
  open() {
    this.store.error.set('');
    this.application = this.emptyApplication();
    this.form?.resetForm(this.application);
    this.dialog.nativeElement.showModal();
  }
  close() { if (!this.store.busy()) this.dialog.nativeElement.close(); }
  cancel(event: Event) { if (this.store.busy()) event.preventDefault(); }
  create() {
    if (!this.form || this.form.invalid) return;
    void this.store.run(async () => {
      await this.store.api.post('/applications', this.application);
      this.dialog.nativeElement.close();
      await this.store.load();
    });
  }
}
