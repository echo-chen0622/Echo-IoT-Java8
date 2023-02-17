import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityTabsComponent } from '../../components/entity/entity-tabs.component';
import { TenantProfile } from '@shared/models/tenant.model';

@Component({
  selector: 'tb-tenant-profile-tabs',
  templateUrl: './tenant-profile-tabs.component.html',
  styleUrls: []
})
export class TenantProfileTabsComponent extends EntityTabsComponent<TenantProfile> {

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }

}
