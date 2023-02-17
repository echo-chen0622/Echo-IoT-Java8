import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeComponentsModule } from '@modules/home/components/home-components.module';
import { TenantProfileRoutingModule } from './tenant-profile-routing.module';
import { TenantProfileTabsComponent } from './tenant-profile-tabs.component';

@NgModule({
  declarations: [
    TenantProfileTabsComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    TenantProfileRoutingModule
  ]
})
export class TenantProfileModule { }
