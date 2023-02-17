import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {Authority} from '@shared/models/authority.enum';
import {ApiUsageComponent} from '@home/pages/api-usage/api-usage.component';

const routes: Routes = [
  {
    path: 'usage',
    component: ApiUsageComponent,
    data: {
      auth: [Authority.TENANT_ADMIN],
      title: 'api-usage.api-usage',
      breadcrumb: {
        label: 'api-usage.api-usage',
        icon: 'insert_chart'
      }
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ApiUsageRoutingModule { }
