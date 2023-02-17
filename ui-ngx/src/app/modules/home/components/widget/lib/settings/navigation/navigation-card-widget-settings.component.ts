import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-navigation-card-widget-settings',
  templateUrl: './navigation-card-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class NavigationCardWidgetSettingsComponent extends WidgetSettingsComponent {

  navigationCardWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.navigationCardWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      name: '{i18n:device.devices}',
      icon: 'devices_other',
      path: '/devices'
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.navigationCardWidgetSettingsForm = this.fb.group({
      name: [settings.name, [Validators.required]],
      icon: [settings.icon, [Validators.required]],
      path: [settings.path, [Validators.required]]
    });
  }
}
