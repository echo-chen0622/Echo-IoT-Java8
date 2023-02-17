import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {SharedModule} from '@shared/shared.module';
import {HomeDialogsModule} from '@home/dialogs/home-dialogs.module';
import {HomeComponentsModule} from '@home/components/home-components.module';
import {EdgeRoutingModule} from '@home/pages/edge/edge-routing.module';
import {EdgeComponent} from '@modules/home/pages/edge/edge.component';
import {EdgeTableHeaderComponent} from '@home/pages/edge/edge-table-header.component';
import {EdgeTabsComponent} from '@home/pages/edge/edge-tabs.component';

@NgModule({
  declarations: [
    EdgeComponent,
    EdgeTableHeaderComponent,
    EdgeTabsComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    HomeDialogsModule,
    HomeComponentsModule,
    EdgeRoutingModule
  ]
})

export class EdgeModule { }
