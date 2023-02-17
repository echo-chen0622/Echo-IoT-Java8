import {Component, OnInit} from '@angular/core';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {PageComponent} from '@shared/components/page.component';
import apiUsageDashboardJson from '!raw-loader!./api_usage_json.raw';
import {Dashboard} from '@shared/models/dashboard.models';

@Component({
  selector: 'tb-api-usage',
  templateUrl: './api-usage.component.html',
  styleUrls: ['./api-usage.component.scss']
})
export class ApiUsageComponent extends PageComponent implements OnInit {

  apiUsageDashboard: Dashboard;

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    this.apiUsageDashboard = JSON.parse(apiUsageDashboardJson);
  }

}
