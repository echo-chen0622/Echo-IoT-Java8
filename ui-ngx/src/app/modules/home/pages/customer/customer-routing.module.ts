import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { EntitiesTableComponent } from '../../components/entity/entities-table.component';
import { Authority } from '@shared/models/authority.enum';
import { UsersTableConfigResolver } from '../user/users-table-config.resolver';
import { CustomersTableConfigResolver } from './customers-table-config.resolver';
import { DevicesTableConfigResolver } from '@modules/home/pages/device/devices-table-config.resolver';
import { AssetsTableConfigResolver } from '../asset/assets-table-config.resolver';
import { DashboardsTableConfigResolver } from '@modules/home/pages/dashboard/dashboards-table-config.resolver';
import { DashboardPageComponent } from '@home/components/dashboard-page/dashboard-page.component';
import { BreadCrumbConfig } from '@shared/components/breadcrumb';
import { dashboardBreadcumbLabelFunction, DashboardResolver } from '@home/pages/dashboard/dashboard-routing.module';
import { EdgesTableConfigResolver } from '@home/pages/edge/edges-table-config.resolver';
import { EntityDetailsPageComponent } from '@home/components/entity/entity-details-page.component';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { entityDetailsPageBreadcrumbLabelFunction } from '@home/pages/home-pages.models';

const routes: Routes = [
  {
    path: 'customers',
    data: {
      breadcrumb: {
        label: 'customer.customers',
        icon: 'supervisor_account'
      }
    },
    children: [
      {
        path: '',
        component: EntitiesTableComponent,
        data: {
          auth: [Authority.TENANT_ADMIN],
          title: 'customer.customers'
        },
        resolve: {
          entitiesTableConfig: CustomersTableConfigResolver
        }
      },
      {
        path: ':entityId',
        component: EntityDetailsPageComponent,
        canDeactivate: [ConfirmOnExitGuard],
        data: {
          breadcrumb: {
            labelFunction: entityDetailsPageBreadcrumbLabelFunction,
            icon: 'supervisor_account'
          } as BreadCrumbConfig<EntityDetailsPageComponent>,
          auth: [Authority.TENANT_ADMIN],
          title: 'customer.customers'
        },
        resolve: {
          entitiesTableConfig: CustomersTableConfigResolver
        }
      },
      {
        path: ':customerId/users',
        data: {
          breadcrumb: {
            label: 'user.customer-users',
            icon: 'account_circle'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'user.customer-users'
            },
            resolve: {
              entitiesTableConfig: UsersTableConfigResolver
            }
          },
          {
            path: ':entityId',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'account_circle'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.TENANT_ADMIN],
              title: 'user.customer-users'
            },
            resolve: {
              entitiesTableConfig: UsersTableConfigResolver
            }
          }
        ]
      },
      {
        path: ':customerId/devices',
        data: {
          breadcrumb: {
            label: 'customer.devices',
            icon: 'devices_other'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'customer.devices',
              devicesType: 'customer'
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
              auth: [Authority.TENANT_ADMIN],
              title: 'customer.devices',
              devicesType: 'customer'
            },
            resolve: {
              entitiesTableConfig: DevicesTableConfigResolver
            }
          }
        ]
      },
      {
        path: ':customerId/assets',
        data: {
          breadcrumb: {
            label: 'customer.assets',
            icon: 'domain'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'customer.assets',
              assetsType: 'customer'
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
              auth: [Authority.TENANT_ADMIN],
              title: 'customer.assets',
              assetsType: 'customer'
            },
            resolve: {
              entitiesTableConfig: AssetsTableConfigResolver
            }
          }
        ]
      },
      {
        path: ':customerId/edgeInstances',
        data: {
          breadcrumb: {
            label: 'customer.edges',
            icon: 'router'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'customer.edges',
              edgesType: 'customer'
            },
            resolve: {
              entitiesTableConfig: EdgesTableConfigResolver
            }
          },
          {
            path: ':entityId',
            component: EntityDetailsPageComponent,
            canDeactivate: [ConfirmOnExitGuard],
            data: {
              breadcrumb: {
                labelFunction: entityDetailsPageBreadcrumbLabelFunction,
                icon: 'router'
              } as BreadCrumbConfig<EntityDetailsPageComponent>,
              auth: [Authority.TENANT_ADMIN],
              title: 'customer.edges',
              edgesType: 'customer'
            },
            resolve: {
              entitiesTableConfig: EdgesTableConfigResolver
            }
          }
        ]
      },
      {
        path: ':customerId/dashboards',
        data: {
          breadcrumb: {
            label: 'customer.dashboards',
            icon: 'dashboard'
          }
        },
        children: [
          {
            path: '',
            component: EntitiesTableComponent,
            data: {
              auth: [Authority.TENANT_ADMIN],
              title: 'customer.dashboards',
              dashboardsType: 'customer'
            },
            resolve: {
              entitiesTableConfig: DashboardsTableConfigResolver
            }
          },
          {
            path: ':dashboardId',
            component: DashboardPageComponent,
            data: {
              breadcrumb: {
                labelFunction: dashboardBreadcumbLabelFunction,
                icon: 'dashboard'
              } as BreadCrumbConfig<DashboardPageComponent>,
              auth: [Authority.TENANT_ADMIN, Authority.CUSTOMER_USER],
              title: 'customer.dashboard',
              widgetEditMode: false
            },
            resolve: {
              dashboard: DashboardResolver
            }
          }
        ]
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: [
    CustomersTableConfigResolver
  ]
})
export class CustomerRoutingModule { }
