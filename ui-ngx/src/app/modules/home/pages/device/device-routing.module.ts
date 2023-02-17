import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {EntitiesTableComponent} from '../../components/entity/entities-table.component';
import {Authority} from '@shared/models/authority.enum';
import {DevicesTableConfigResolver} from '@modules/home/pages/device/devices-table-config.resolver';
import {EntityDetailsPageComponent} from '@home/components/entity/entity-details-page.component';
import {ConfirmOnExitGuard} from '@core/guards/confirm-on-exit.guard';
import {entityDetailsPageBreadcrumbLabelFunction} from '@home/pages/home-pages.models';
import {BreadCrumbConfig} from '@shared/components/breadcrumb';

const routes: Routes = [
  {
    path: 'devices',
    data: {
      breadcrumb: {
        label: 'device.devices',
        icon: 'devices_other'
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'device.devices',
          devicesType: 'tenant'
        },
        resolve: {
          entitiesTableConfig: DevicesTableConfigResolver
        }
      },
      {
        path: ':entityId',
        component: EntityDetailsPageComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          breadcrumb: {
            labelFunction: entityDetailsPageBreadcrumbLabelFunction,
            icon: 'devices_other'
          } as BreadCrumbConfig<EntityDetailsPageComponent>,
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'device.devices',
          devicesType: 'tenant'
        },
        resolve: {
          entitiesTableConfig: DevicesTableConfigResolver
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    DevicesTableConfigResolver
  ]
})
export class DeviceRoutingModule { }
