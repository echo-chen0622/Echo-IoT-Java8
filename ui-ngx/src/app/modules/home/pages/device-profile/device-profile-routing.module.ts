import {NgModule} from '@angular/core';
import {Routes} from '@angular/router';

import {EntitiesTableComponent} from '../../components/entity/entities-table.component';
import {Authority} from '@shared/models/authority.enum';
import {DeviceProfilesTableConfigResolver} from './device-profiles-table-config.resolver';
import {EntityDetailsPageComponent} from '@home/components/entity/entity-details-page.component';
import {ConfirmOnExitGuard} from '@core/guards/confirm-on-exit.guard';
import {entityDetailsPageBreadcrumbLabelFunction} from '@home/pages/home-pages.models';
import {BreadCrumbConfig} from '@shared/components/breadcrumb';

export const deviceProfilesRoutes: Routes = [
  {
    path: 'deviceProfiles',
    data: {
      breadcrumb: {
        label: 'device-profile.device-profiles',
        icon: 'mdi:alpha-d-box'
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'device-profile.device-profiles'
        },
        resolve: {
          entitiesTableConfig: DeviceProfilesTableConfigResolver
        }
      },
      {
        path: ':entityId',
        component: EntityDetailsPageComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          breadcrumb: {
            labelFunction: entityDetailsPageBreadcrumbLabelFunction,
            icon: 'mdi:alpha-d-box'
          } as BreadCrumbConfig<EntityDetailsPageComponent>,
          auth: [Authority.TENANT_ADMIN],
          title: 'device-profile.device-profiles'
        },
        resolve: {
          entitiesTableConfig: DeviceProfilesTableConfigResolver
        }
      }
    ]
  }
];

@NgModule({
  providers: [
    DeviceProfilesTableConfigResolver
  ]
})
export class DeviceProfileRoutingModule { }
