import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-update-boolean-attribute-widget-settings',
  templateUrl: './update-boolean-attribute-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class UpdateBooleanAttributeWidgetSettingsComponent extends WidgetSettingsComponent {

  updateBooleanAttributeWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.updateBooleanAttributeWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      widgetTitle: '',
      showResultMessage: true,
      trueValue: '',
      falseValue: ''
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.updateBooleanAttributeWidgetSettingsForm = this.fb.group({
      widgetTitle: [settings.widgetTitle, []],
      showResultMessage: [settings.showResultMessage, []],
      trueValue: [settings.trueValue, []],
      falseValue: [settings.falseValue, []]
    });
  }
}
