import {
  Component,
  ElementRef,
  HostListener,
  ViewChild,
  computed,
  forwardRef,
  inject,
  input,
  signal,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export interface SelectOption {
  value: number;
  label: string;
}
let nextId = 0;

@Component({
  selector: 'app-select',
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => Select), multi: true }],
  templateUrl: './select.html',
  styleUrl: './select.scss',
})
export class Select implements ControlValueAccessor {
  readonly options = input<readonly SelectOption[]>([]);
  readonly placeholder = input('Selecione uma opção');
  readonly ariaLabel = input('Selecionar opção');
  readonly searchThreshold = input(8);
  readonly required = input(false);
  readonly searchable = computed(() => this.options().length >= this.searchThreshold());
  readonly value = signal<number | null>(null);
  readonly disabled = signal(false);
  readonly opened = signal(false);
  readonly query = signal('');
  readonly activeIndex = signal(0);
  readonly selected = computed(() =>
    this.options().find((option) => option.value === this.value()),
  );
  readonly filtered = computed(() => {
    const query = this.normalize(this.query());
    return this.options().filter((option) => this.normalize(option.label).includes(query));
  });
  readonly id = `select-${++nextId}`;
  readonly position = signal({
    top: '0px',
    bottom: 'auto',
    left: '0px',
    width: '0px',
    maxHeight: '280px',
  });
  private readonly host = inject(ElementRef<HTMLElement>);
  @ViewChild('control', { static: true }) control!: ElementRef<HTMLInputElement>;
  @ViewChild('popup', { static: true }) popup!: ElementRef<HTMLElement>;
  private change: (value: number | null) => void = () => {};
  private touched: () => void = () => {};
  private normalize(value: string) {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLocaleLowerCase('pt-BR')
      .trim();
  }
  writeValue(value: number | null) {
    this.value.set(value);
  }
  registerOnChange(fn: (value: number | null) => void) {
    this.change = fn;
  }
  registerOnTouched(fn: () => void) {
    this.touched = fn;
  }
  setDisabledState(disabled: boolean) {
    this.disabled.set(disabled);
    if (disabled) this.close();
  }
  open() {
    if (this.disabled() || this.opened()) return;
    this.query.set('');
    this.activeIndex.set(
      Math.max(
        0,
        this.options().findIndex((option) => option.value === this.value()),
      ),
    );
    this.opened.set(true);
    const rect = this.control.nativeElement.getBoundingClientRect();
    const below = window.innerHeight - rect.bottom - 12;
    const above = rect.top - 12;
    const desiredHeight = Math.min(280, this.options().length * 44 + (this.searchable() ? 48 : 12));
    const height = Math.max(44, Math.min(desiredHeight, Math.max(below, above)));
    const upwards = below < desiredHeight && above > below;
    this.position.set({
      top: upwards ? 'auto' : `${rect.bottom + 6}px`,
      bottom: upwards ? `${window.innerHeight - rect.top + 6}px` : 'auto',
      left: `${rect.left}px`,
      width: `${rect.width}px`,
      maxHeight: `${height}px`,
    });
    this.popup.nativeElement.showPopover();
    this.scrollActive();
  }
  close() {
    this.opened.set(false);
    this.query.set('');
    this.popup?.nativeElement.hidePopover();
  }
  blur() {
    this.touched();
    this.close();
  }
  search(value: string) {
    this.open();
    this.query.set(value);
    this.activeIndex.set(0);
    this.scrollActive();
  }
  choose(option: SelectOption, event?: Event) {
    // Cancel implicit activation of the surrounding form label, which would
    // send another click to the input and reopen the popover after selection.
    event?.preventDefault();
    event?.stopPropagation();
    if (this.disabled()) return;
    this.value.set(option.value);
    this.close();
    this.change(option.value);
    this.touched();
  }
  keydown(event: KeyboardEvent) {
    if (event.key === 'Escape' && this.opened()) {
      event.preventDefault();
      event.stopPropagation();
      this.close();
      return;
    }
    if (event.key === 'Tab') {
      this.close();
      return;
    }
    if (
      !this.opened() &&
      this.searchable() &&
      !event.ctrlKey &&
      !event.metaKey &&
      !event.altKey &&
      (event.key.length === 1 || event.key === 'Backspace' || event.key === 'Delete')
    ) {
      event.preventDefault();
      this.open();
      this.query.set(event.key.length === 1 ? event.key : '');
      this.activeIndex.set(0);
      this.scrollActive();
      return;
    }
    if (event.key === 'Enter' || (event.key === ' ' && !this.searchable())) {
      event.preventDefault();
      if (!this.opened()) this.open();
      else {
        const option = this.filtered()[this.activeIndex()];
        if (option) this.choose(option);
      }
      return;
    }
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      if (!this.opened()) {
        this.open();
        return;
      }
      const count = this.filtered().length;
      if (count)
        this.activeIndex.set(
          (this.activeIndex() + (event.key === 'ArrowDown' ? 1 : -1) + count) % count,
        );
      this.scrollActive();
    }
  }
  private scrollActive() {
    setTimeout(() =>
      this.popup.nativeElement
        .querySelector('.active-option')
        ?.scrollIntoView({ block: 'nearest' }),
    );
  }
  @HostListener('document:pointerdown', ['$event'])
  outside(event: PointerEvent) {
    if (!this.host.nativeElement.contains(event.target as Node)) this.close();
  }
  @HostListener('window:resize')
  @HostListener('window:scroll')
  reposition() {
    this.close();
  }
}
