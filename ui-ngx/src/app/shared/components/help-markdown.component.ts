import {
  Component,
  EventEmitter,
  Input, OnChanges,
  OnDestroy, OnInit,
  Output, SimpleChanges
} from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { share } from 'rxjs/operators';
import { HelpService } from '@core/services/help.service';

@Component({
  selector: 'tb-help-markdown',
  templateUrl: './help-markdown.component.html',
  styleUrls: ['./help-markdown.component.scss']
})
export class HelpMarkdownComponent implements OnDestroy, OnInit, OnChanges {

  @Input() helpId: string;

  @Input() helpContent: string;

  @Input() visible: boolean;

  @Input() style: { [klass: string]: any } = {};

  @Output() markdownReady = new EventEmitter<void>();

  markdownText = new BehaviorSubject<string>(null);

  markdownText$ = this.markdownText.pipe(
    share()
  );

  private loadHelpPending = false;

  constructor(private help: HelpService) {}

  ngOnInit(): void {
    this.loadHelpWhenVisible();
  }

  ngOnDestroy(): void {
    this.markdownText.complete();
  }

  ngOnChanges(changes: SimpleChanges): void {
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (propName === 'visible') {
          if (this.loadHelpPending) {
            this.loadHelpPending = false;
            this.loadHelp();
          }
        }
        if (propName === 'helpId' || propName === 'helpContent') {
          this.markdownText.next(null);
          this.loadHelpWhenVisible();
        }
      }
    }
  }

  private loadHelpWhenVisible() {
    if (this.visible) {
      this.loadHelp();
    } else {
      this.loadHelpPending = true;
    }
  }

  private loadHelp() {
    if (this.helpId) {
      this.help.getHelpContent(this.helpId).subscribe((content) => {
        this.markdownText.next(content);
      });
    } else if (this.helpContent) {
      this.markdownText.next(this.helpContent);
    }
  }

  onMarkdownReady() {
    this.markdownReady.next();
  }

  markdownClick($event: MouseEvent) {
  }

}
