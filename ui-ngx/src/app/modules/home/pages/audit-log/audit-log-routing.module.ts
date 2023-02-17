import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {Authority} from '@shared/models/authority.enum';
import {AuditLogTableComponent} from '@home/components/audit-log/audit-log-table.component';

const routes: Routes = [
  {
    path: 'auditLogs',
    component: AuditLogTableComponent,
    data: {
      auth: [Authority.TENANT_ADMIN],
      title: 'audit-log.audit-logs',
      breadcrumb: {
        label: 'audit-log.audit-logs',
        icon: 'track_changes'
      },
      isPage: true
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class AuditLogRoutingModule { }
