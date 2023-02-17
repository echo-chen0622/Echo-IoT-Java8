import {Injectable, NgModule} from '@angular/core';
import {Resolve, RouterModule, Routes} from '@angular/router';

import {HomeLinksComponent} from './home-links.component';
import {Authority} from '@shared/models/authority.enum';
import {Observable} from 'rxjs';
import {HomeDashboard} from '@shared/models/dashboard.models';
import {DashboardService} from '@core/http/dashboard.service';

@Injectable()
export class HomeDashboardResolver implements Resolve<HomeDashboard> {

  constructor(private dashboardService: DashboardService) {
  }

  resolve(): Observable<HomeDashboard> {
    return this.dashboardService.getHomeDashboard();
  }
}

const routes: Routes = [
  {
    path: 'home',
    component: HomeLinksComponent,
    data: {
      auth: [Authority.SYS_ADMIN, Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
      title: 'home.home',
      breadcrumb: {
        label: 'home.home',
        icon: 'home'
      }
    },
    resolve: {
      homeDashboard: HomeDashboardResolver
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    HomeDashboardResolver
  ]
})
export class HomeLinksRoutingModule { }
