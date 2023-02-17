import {Component} from '@angular/core';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {EntityTabsComponent} from '../../components/entity/entity-tabs.component';
import {Dashboard} from '@shared/models/dashboard.models';

@Component({
  selector: 'tb-dashboard-tabs',
  templateUrl: './dashboard-tabs.component.html',
  styleUrls: []
})
export class DashboardTabsComponent extends EntityTabsComponent<Dashboard> {

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }

}
