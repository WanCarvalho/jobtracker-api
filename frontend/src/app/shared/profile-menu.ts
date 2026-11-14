import { Component, ElementRef, HostListener, ViewChild, inject, signal } from '@angular/core';
import { TrackerStore } from '../tracker.store';

@Component({
  selector: 'app-profile-menu',
  templateUrl: './profile-menu.html',
  styleUrl: './profile-menu.scss',
})
export class ProfileMenu {
  readonly store = inject(TrackerStore);
  private readonly host = inject(ElementRef<HTMLElement>);
  @ViewChild('menu', { static: true }) menu!: ElementRef<HTMLDetailsElement>;
  @ViewChild('imageDialog', { static: true }) dialog!: ElementRef<HTMLDialogElement>;
  @ViewChild('imageInput', { static: true }) input!: ElementRef<HTMLInputElement>;
  readonly draft = signal<string | null>(null);
  readonly reading = signal(false);
  readonly imageError = signal('');

  @HostListener('document:pointerdown', ['$event'])
  closeOutside(event: PointerEvent) {
    if (!this.host.nativeElement.contains(event.target as Node))
      this.menu.nativeElement.open = false;
  }

  @HostListener('document:keydown.escape')
  closeMenu() {
    if (!this.menu.nativeElement.open) return;
    this.menu.nativeElement.open = false;
    this.menu.nativeElement.querySelector('summary')?.focus();
  }

  openImageEditor() {
    this.menu.nativeElement.open = false;
    this.store.error.set('');
    this.imageError.set('');
    this.draft.set(this.store.api.session()?.profileImage ?? null);
    this.input.nativeElement.value = '';
    this.dialog.nativeElement.showModal();
  }

  closeEditor() {
    if (!this.store.busy() && !this.reading()) this.dialog.nativeElement.close();
  }

  cancel(event: Event) {
    if (this.store.busy() || this.reading()) event.preventDefault();
  }

  async selectImage(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    this.imageError.set('');
    if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type)) {
      this.imageError.set('Selecione uma imagem PNG, JPEG ou WebP.');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.imageError.set('A imagem deve ter no máximo 5 MB.');
      return;
    }

    this.reading.set(true);
    const url = URL.createObjectURL(file);
    try {
      const image = new Image();
      image.src = url;
      await image.decode();
      const canvas = document.createElement('canvas');
      canvas.width = 256;
      canvas.height = 256;
      const context = canvas.getContext('2d');
      if (!context) throw new Error('Canvas indisponível');
      const size = Math.min(image.naturalWidth, image.naturalHeight);
      context.drawImage(
        image,
        (image.naturalWidth - size) / 2,
        (image.naturalHeight - size) / 2,
        size,
        size,
        0,
        0,
        256,
        256,
      );
      this.draft.set(canvas.toDataURL('image/png'));
    } catch {
      this.imageError.set('Não foi possível abrir a imagem. Selecione outro arquivo.');
    } finally {
      URL.revokeObjectURL(url);
      this.reading.set(false);
    }
  }

  save() {
    const image = this.draft();
    if (
      !image ||
      this.reading() ||
      this.imageError() ||
      image === this.store.api.session()?.profileImage
    )
      return;
    void this.store.run(async () => {
      await this.store.api.updateProfileImage(image);
      this.dialog.nativeElement.close();
    });
  }

  logout() {
    this.menu.nativeElement.open = false;
    this.store.logout();
  }
}
