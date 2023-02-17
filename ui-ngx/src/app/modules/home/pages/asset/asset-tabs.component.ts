import {Component} from '@angular/core';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {EntityTabsComponent} from '../../components/entity/entity-tabs.component';
import {AssetInfo} from '@app/shared/models/asset.models';

@Component({
  selector: 'tb-asset-tabs',
  templateUrl: './asset-tabs.component.html',
  styleUrls: []
})
export class AssetTabsComponent extends EntityTabsComponent<AssetInfo> {

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }

}
