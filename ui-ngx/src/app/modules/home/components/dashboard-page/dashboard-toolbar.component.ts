import {Component, EventEmitter, Input, OnInit, Output, ViewEncapsulation} from '@angular/core';

@Component({
  selector: 'tb-dashboard-toolbar',
  templateUrl: './dashboard-toolbar.component.html',
  styleUrls: ['./dashboard-toolbar.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class DashboardToolbarComponent implements OnInit {

  @Input()
  toolbarOpened: boolean;

  @Input()
  forceFullscreen: boolean;

  @Output()
  triggerClick = new EventEmitter<void>();

  constructor() {
  }

  ngOnInit(): void {
  }

  onTriggerClick() {
    this.triggerClick.emit();
  }

}
