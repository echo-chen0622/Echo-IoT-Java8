import { Component, ViewContainerRef } from '@angular/core';

@Component({
  selector: 'tb-anchor',
  template: '<ng-template></ng-template>'
})
export class TbAnchorComponent {
  constructor(public viewContainerRef: ViewContainerRef) { }
}
