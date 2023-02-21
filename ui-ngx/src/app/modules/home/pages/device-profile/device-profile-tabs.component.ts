import {Component} from '@angular/core';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {EntityTabsComponent} from '../../components/entity/entity-tabs.component';
import {
  DeviceProfile,
  DeviceTransportType,
  deviceTransportTypeHintMap,
  deviceTransportTypeTranslationMap
} from '@shared/models/device.models';

@Component({
  selector: 'tb-device-profile-tabs',
  templateUrl: './device-profile-tabs.component.html',
  styleUrls: []
})
export class DeviceProfileTabsComponent extends EntityTabsComponent<DeviceProfile> {

  deviceTransportTypes = Object.values(DeviceTransportType);

  deviceTransportTypeTranslations = deviceTransportTypeTranslationMap;

  deviceTransportTypeHints = deviceTransportTypeHintMap;

  isTransportTypeChanged = false;

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
    super.ngOnInit();
    this.detailsForm.get('transportType').valueChanges.subscribe(() => {
      this.isTransportTypeChanged = true;
    });
  }

}
