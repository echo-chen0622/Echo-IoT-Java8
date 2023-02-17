import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { MenuSection } from '@core/services/menu.models';

@Component({
  selector: 'tb-menu-link',
  templateUrl: './menu-link.component.html',
  styleUrls: ['./menu-link.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MenuLinkComponent implements OnInit {

  @Input() section: MenuSection;

  constructor() {
  }

  ngOnInit() {
  }

}
