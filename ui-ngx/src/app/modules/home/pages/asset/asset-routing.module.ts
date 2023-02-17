import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {EntitiesTableComponent} from '../../components/entity/entities-table.component';
import {Authority} from '@shared/models/authority.enum';
import {AssetsTableConfigResolver} from './assets-table-config.resolver';
import {EntityDetailsPageComponent} from '@home/components/entity/entity-details-page.component';
import {BreadCrumbConfig} from '@shared/components/breadcrumb';
import {ConfirmOnExitGuard} from '@core/guards/confirm-on-exit.guard';
import {entityDetailsPageBreadcrumbLabelFunction} from '@home/pages/home-pages.models';

const routes: Routes = [
  {
    path: 'assets',
    data: {
      breadcrumb: {
        label: 'asset.assets',
        icon: 'domain'
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'asset.assets',
          assetsType: 'tenant'
        },
        resolve: {
          entitiesTableConfig: AssetsTableConfigResolver
        }
      },
      {
        path: ':entityId',
        component: EntityDetailsPageComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          breadcrumb: {
            labelFunction: entityDetailsPageBreadcrumbLabelFunction,
            icon: 'domain'
          } as BreadCrumbConfig<EntityDetailsPageComponent>,
          auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
          title: 'asset.assets',
          assetsType: 'tenant'
        },
        resolve: {
          entitiesTableConfig: AssetsTableConfigResolver
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    AssetsTableConfigResolver
  ]
})
export class AssetRoutingModule { }
