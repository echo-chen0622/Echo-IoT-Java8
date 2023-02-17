import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { MenuSection } from '@core/services/menu.models';
import { Router } from '@angular/router';

@Component({
  selector: 'tb-menu-toggle',
  templateUrl: './menu-toggle.component.html',
  styleUrls: ['./menu-toggle.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MenuToggleComponent implements OnInit {

  @Input() section: MenuSection;

  constructor(private router: Router) {
  }

  ngOnInit() {
  }

  sectionActive(): boolean {
    return this.router.isActive(this.section.path, false);
  }

  sectionHeight(): string {
    if (this.router.isActive(this.section.path, false)) {
      return this.section.height;
    } else {
      return '0px';
    }
  }

  trackBySectionPages(index: number, section: MenuSection){
    return section.id;
  }
}
