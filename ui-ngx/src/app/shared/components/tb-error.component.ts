import { Component, Input } from '@angular/core';
import { animate, state, style, transition, trigger } from '@angular/animations';

@Component({
  selector: 'tb-error',
  template: `
  <div [@animation]="state" style="margin-top:0.5rem;font-size:.75rem">
      <mat-error >
      {{message}}
    </mat-error>
    </div>
  `,
  styles: [`
    :host {
        height: 24px;
    }
  `],
  animations: [
    trigger('animation', [
      state('show', style({
        opacity: 1,
      })),
      state('hide',   style({
        opacity: 0,
        transform: 'translateY(-1rem)'
      })),
      transition('show => hide', animate('200ms ease-out')),
      transition('* => show', animate('200ms ease-in'))

    ]),
  ]
})
export class TbErrorComponent {
  errorValue: any;
  state: any;
  message;

  @Input()
  set error(value) {
    if (value && !this.message) {
      this.message = value;
      this.state = 'hide';
      setTimeout(() => {
        this.state = 'show';
      });
    } else {
      this.errorValue = value;
      this.state = value ? 'show' : 'hide';
    }
  }
}
