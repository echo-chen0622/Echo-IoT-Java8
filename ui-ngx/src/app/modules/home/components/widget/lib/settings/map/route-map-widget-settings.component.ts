import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { defaultMapSettings } from 'src/app/modules/home/components/widget/lib/maps/map-models';

@Component({
  selector: 'tb-route-map-widget-settings',
  templateUrl: './route-map-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class RouteMapWidgetSettingsComponent extends WidgetSettingsComponent {

  routeMapWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.routeMapWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      ...defaultMapSettings
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.routeMapWidgetSettingsForm = this.fb.group({
      mapSettings: [settings.mapSettings, []]
    });
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    return {
      mapSettings: settings
    };
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return settings.mapSettings;
  }
}
