import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharedModule } from '@app/shared/shared.module';
import { AlarmDetailsDialogComponent } from '@home/components/alarm/alarm-details-dialog.component';
import { SHARED_HOME_COMPONENTS_MODULE_TOKEN } from '@home/components/tokens';

@NgModule({
  providers: [
    { provide: SHARED_HOME_COMPONENTS_MODULE_TOKEN, useValue: SharedHomeComponentsModule }
  ],
  declarations:
    [
      AlarmDetailsDialogComponent
    ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    AlarmDetailsDialogComponent
  ]
})
export class SharedHomeComponentsModule { }
