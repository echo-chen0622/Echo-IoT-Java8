import {Component} from '@angular/core';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {EntityTabsComponent} from '../../components/entity/entity-tabs.component';
import {AssetProfile} from '@shared/models/asset.models';

@Component({
  selector: 'tb-asset-profile-tabs',
  templateUrl: './asset-profile-tabs.component.html',
  styleUrls: []
})
export class AssetProfileTabsComponent extends EntityTabsComponent<AssetProfile> {

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }

}
