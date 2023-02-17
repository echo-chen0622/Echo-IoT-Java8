import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-simple-card-widget-settings',
  templateUrl: './simple-card-widget-settings.component.html',
  styleUrls: []
})
export class SimpleCardWidgetSettingsComponent extends WidgetSettingsComponent {

  simpleCardWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.simpleCardWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      labelPosition: 'left'
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.simpleCardWidgetSettingsForm = this.fb.group({
      labelPosition: [settings.labelPosition, []]
    });
  }
}
