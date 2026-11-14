import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TrackerStore } from './tracker.store';
import { ProfileMenu } from './shared/profile-menu';

@Component({
  selector: 'app-root',
  imports: [FormsModule, RouterLink, RouterLinkActive, RouterOutlet, ProfileMenu],
  templateUrl: './app.html',
})
export class App {
  readonly store = inject(TrackerStore);
}
