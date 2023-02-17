import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@shared/shared.module';
import { HomeComponentsModule } from '@home/components/home-components.module';
import { OtaUpdateRoutingModule } from '@home/pages/ota-update/ota-update-routing.module';
import { OtaUpdateComponent } from '@home/pages/ota-update/ota-update.component';

@NgModule({
  declarations: [
    OtaUpdateComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    OtaUpdateRoutingModule
  ]
})
export class OtaUpdateModule { }
