import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {SharedModule} from '@app/shared/shared.module';
import {HomeComponentsModule} from '@modules/home/components/home-components.module';
import {ApiUsageComponent} from '@home/pages/api-usage/api-usage.component';
import {ApiUsageRoutingModule} from '@home/pages/api-usage/api-usage-routing.module';

@NgModule({
  declarations:
    [
      ApiUsageComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    ApiUsageRoutingModule
  ]
})
export class ApiUsageModule { }
