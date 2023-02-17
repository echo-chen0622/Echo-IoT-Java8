import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { deepClone } from '@core/utils';
import {
  updateAttributeGeneralDefaultSettings
} from '@home/components/widget/lib/settings/input/update-attribute-general-settings.component';

@Component({
  selector: 'tb-update-string-attribute-widget-settings',
  templateUrl: './update-string-attribute-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class UpdateStringAttributeWidgetSettingsComponent extends WidgetSettingsComponent {

  updateStringAttributeWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.updateStringAttributeWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      ...updateAttributeGeneralDefaultSettings(),
      minLength: null,
      maxLength: null,
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.updateStringAttributeWidgetSettingsForm = this.fb.group({
      updateAttributeGeneralSettings: [settings.updateAttributeGeneralSettings, []],
      minLength: [settings.minLength, [Validators.min(0)]],
      maxLength: [settings.maxLength, [Validators.min(1)]]
    });
  }

  protected prepareInputSettings(settings: WidgetSettings): WidgetSettings {
    const updateAttributeGeneralSettings = deepClone(settings, ['minLength', 'maxLength']);
    return {
      updateAttributeGeneralSettings,
      minLength: settings.minLength,
      maxLength: settings.maxLength
    };
  }

  protected prepareOutputSettings(settings: any): WidgetSettings {
    return {
      ...settings.updateAttributeGeneralSettings,
      minLength: settings.minLength,
      maxLength: settings.maxLength
    };
  }
}
