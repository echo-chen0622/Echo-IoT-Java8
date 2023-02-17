import {Component} from '@angular/core';
import {WidgetSettings, WidgetSettingsComponent} from '@shared/models/widget.models';
import {FormBuilder, FormGroup} from '@angular/forms';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {flotDataKeyDefaultSettings} from '@home/components/widget/lib/settings/chart/flot-key-settings.component';

@Component({
  selector: 'tb-flot-bar-key-settings',
  templateUrl: './flot-bar-key-settings.component.html',
  styleUrls: []
})
export class FlotBarKeySettingsComponent extends WidgetSettingsComponent {

  flotBarKeySettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.flotBarKeySettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return flotDataKeyDefaultSettings('bar');
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.flotBarKeySettingsForm = this.fb.group({
      flotKeySettings: [settings.flotKeySettings, []]
    });
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    return {
      flotKeySettings: settings
    };
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return settings.flotKeySettings;
  }
}
