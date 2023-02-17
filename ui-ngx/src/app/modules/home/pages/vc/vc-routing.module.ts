import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ConfirmOnExitGuard } from '@core/guards/confirm-on-exit.guard';
import { Authority } from '@shared/models/authority.enum';
import { VersionControlComponent } from '@home/components/vc/version-control.component';

const routes: Routes = [
  {
    path: 'vc',
    component: VersionControlComponent,
    canDeactivate: [ConfirmOnExitGuard],
    data: {
      auth: [Authority.TENANT_ADMIN],
      title: 'version-control.version-control',
      breadcrumb: {
        label: 'version-control.version-control',
        icon: 'history'
      }
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
  providers: []
})
export class VcRoutingModule { }
