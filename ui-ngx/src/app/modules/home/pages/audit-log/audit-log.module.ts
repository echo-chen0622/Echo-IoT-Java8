import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {SharedModule} from '@shared/shared.module';
import {AuditLogRoutingModule} from '@modules/home/pages/audit-log/audit-log-routing.module';

@NgModule({
  declarations: [
  ],
  imports: [
    CommonModule,
    SharedModule,
    AuditLogRoutingModule
  ]
})
export class AuditLogModule { }
