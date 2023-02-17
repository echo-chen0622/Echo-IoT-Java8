import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-update-image-attribute-widget-settings',
  templateUrl: './update-image-attribute-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class UpdateImageAttributeWidgetSettingsComponent extends WidgetSettingsComponent {

  updateImageAttributeWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.updateImageAttributeWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      widgetTitle: '',
      showResultMessage: true,
      displayPreview: true,
      displayClearButton: false,
      displayApplyButton: true,
      displayDiscardButton: true
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.updateImageAttributeWidgetSettingsForm = this.fb.group({
      widgetTitle: [settings.widgetTitle, []],
      showResultMessage: [settings.showResultMessage, []],
      displayPreview: [settings.displayPreview, []],
      displayClearButton: [settings.displayClearButton, []],
      displayApplyButton: [settings.displayApplyButton, []],
      displayDiscardButton: [settings.displayDiscardButton, []]
    });
  }
}
