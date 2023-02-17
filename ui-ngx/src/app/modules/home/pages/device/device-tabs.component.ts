import {Component} from '@angular/core';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {DeviceInfo} from '@shared/models/device.models';
import {EntityTabsComponent} from '../../components/entity/entity-tabs.component';

@Component({
  selector: 'tb-device-tabs',
  templateUrl: './device-tabs.component.html',
  styleUrls: []
})
export class DeviceTabsComponent extends EntityTabsComponent<DeviceInfo> {

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
  }

}
