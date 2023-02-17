import {NgModule} from '@angular/core';

import {AdminModule} from './admin/admin.module';
import {HomeLinksModule} from './home-links/home-links.module';
import {ProfileModule} from './profile/profile.module';
import {SecurityModule} from '@home/pages/security/security.module';
import {TenantModule} from '@modules/home/pages/tenant/tenant.module';
import {CustomerModule} from '@modules/home/pages/customer/customer.module';
import {AuditLogModule} from '@modules/home/pages/audit-log/audit-log.module';
import {UserModule} from '@modules/home/pages/user/user.module';
import {DeviceModule} from '@modules/home/pages/device/device.module';
import {AssetModule} from '@modules/home/pages/asset/asset.module';
import {EntityViewModule} from '@modules/home/pages/entity-view/entity-view.module';
import {RuleChainModule} from '@modules/home/pages/rulechain/rulechain.module';
import {WidgetLibraryModule} from '@modules/home/pages/widget/widget-library.module';
import {DashboardModule} from '@modules/home/pages/dashboard/dashboard.module';
import {TenantProfileModule} from './tenant-profile/tenant-profile.module';
import {DeviceProfileModule} from './device-profile/device-profile.module';
import {ApiUsageModule} from '@home/pages/api-usage/api-usage.module';
import {EdgeModule} from '@home/pages/edge/edge.module';
import {OtaUpdateModule} from '@home/pages/ota-update/ota-update.module';
import {VcModule} from '@home/pages/vc/vc.module';
import {AssetProfileModule} from '@home/pages/asset-profile/asset-profile.module';
import {ProfilesModule} from '@home/pages/profiles/profiles.module';

@NgModule({
  exports: [
    AdminModule,
    HomeLinksModule,
    ProfileModule,
    SecurityModule,
    TenantProfileModule,
    TenantModule,
    DeviceProfileModule,
    AssetProfileModule,
    ProfilesModule,
    DeviceModule,
    AssetModule,
    EdgeModule,
    EntityViewModule,
    CustomerModule,
    RuleChainModule,
    WidgetLibraryModule,
    DashboardModule,
    AuditLogModule,
    ApiUsageModule,
    OtaUpdateModule,
    UserModule,
    VcModule
  ]
})
export class HomePagesModule { }
