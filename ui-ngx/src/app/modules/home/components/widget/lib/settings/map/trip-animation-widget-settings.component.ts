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
