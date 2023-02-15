///
/// Copyright © 2016-2023 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  CircleSettings,
  defaultTripAnimationSettings,
  MapProviderSettings,
  PointsSettings,
  PolygonSettings,
  PolylineSettings,
  TripAnimationCommonSettings,
  TripAnimationMarkerSettings
} from 'src/app/modules/home/components/widget/lib/maps/map-models';
import { extractType } from '@core/utils';
import { keys } from 'ts-transformer-keys';

@Component({
  selector: 'tb-trip-animation-widget-settings',
  templateUrl: './trip-animation-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class TripAnimationWidgetSettingsComponent extends WidgetSettingsComponent {

  tripAnimationWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.tripAnimationWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      ...defaultTripAnimationSettings
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.tripAnimationWidgetSettingsForm = this.fb.group({
      mapProviderSettings: [settings.mapProviderSettings, []],
      commonMapSettings: [settings.commonMapSettings, []],
      markersSettings: [settings.markersSettings, []],
      pathSettings: [settings.pathSettings, []],
      pointSettings: [settings.pointSettings, []],
      polygonSettings: [settings.polygonSettings, []],
      circleSettings: [settings.circleSettings, []]
    });
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    const mapProviderSettings = extractType<MapProviderSettings>(settings, keys<MapProviderSettings>());
    const commonMapSettings = extractType<TripAnimationCommonSettings>(settings, keys<TripAnimationCommonSettings>());
    const markersSettings = extractType<TripAnimationMarkerSettings>(settings, keys<TripAnimationMarkerSettings>());
    const pathSettings = extractType<PolylineSettings>(settings, keys<PolylineSettings>());
    const pointSettings = extractType<PointsSettings>(settings, keys<PointsSettings>());
    const polygonSettings = extractType<PolygonSettings>(settings, keys<PolygonSettings>());
    const circleSettings = extractType<CircleSettings>(settings, keys<CircleSettings>());
    return {
      mapProviderSettings,
      commonMapSettings,
      markersSettings,
      pathSettings,
      pointSettings,
      polygonSettings,
      circleSettings
    };
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return {
      ...settings.mapProviderSettings,
      ...settings.commonMapSettings,
      ...settings.markersSettings,
      ...settings.pathSettings,
      ...settings.pointSettings,
      ...settings.polygonSettings,
      ...settings.circleSettings,
    };
  }
}
