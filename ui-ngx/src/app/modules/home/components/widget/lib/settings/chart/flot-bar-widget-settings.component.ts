import {Component} from '@angular/core';
import {WidgetSettings, WidgetSettingsComponent} from '@shared/models/widget.models';
import {FormBuilder, FormGroup} from '@angular/forms';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {flotDefaultSettings} from '@home/components/widget/lib/settings/chart/flot-widget-settings.component';

@Component({
  selector: 'tb-flot-bar-widget-settings',
  templateUrl: './flot-bar-widget-settings.component.html',
  styleUrls: []
})
export class FlotBarWidgetSettingsComponent extends WidgetSettingsComponent {

  flotBarWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.flotBarWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return flotDefaultSettings('bar');
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.flotBarWidgetSettingsForm = this.fb.group({
      flotSettings: [settings.flotSettings, []]
    });
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    return {
      flotSettings: settings
    };
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return settings.flotSettings;
  }
}
