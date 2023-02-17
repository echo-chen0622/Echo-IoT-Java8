import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { deepClone } from '@core/utils';
import {
  updateAttributeGeneralDefaultSettings
} from '@home/components/widget/lib/settings/input/update-attribute-general-settings.component';

@Component({
  selector: 'tb-update-double-attribute-widget-settings',
  templateUrl: './update-double-attribute-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class UpdateDoubleAttributeWidgetSettingsComponent extends WidgetSettingsComponent {

  updateDoubleAttributeWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.updateDoubleAttributeWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      ...updateAttributeGeneralDefaultSettings(),
      minValue: null,
      maxValue: null
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.updateDoubleAttributeWidgetSettingsForm = this.fb.group({
      updateAttributeGeneralSettings: [settings.updateAttributeGeneralSettings, []],
      minValue: [settings.minValue, []],
      maxValue: [settings.maxValue, []]
    });
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    const updateAttributeGeneralSettings = deepClone(settings, ['minValue', 'maxValue']);
    return {
      updateAttributeGeneralSettings,
      minValue: settings.minValue,
      maxValue: settings.maxValue
    };
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return {
      ...settings.updateAttributeGeneralSettings,
      minValue: settings.minValue,
      maxValue: settings.maxValue
    };
  }
}
