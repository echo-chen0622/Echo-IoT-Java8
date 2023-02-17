import {Component} from '@angular/core';
import {WidgetSettings, WidgetSettingsComponent} from '@shared/models/widget.models';
import {FormBuilder, FormGroup} from '@angular/forms';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';

@Component({
  selector: 'tb-gateway-config-single-device-widget-settings',
  templateUrl: './gateway-config-single-device-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class GatewayConfigSingleDeviceWidgetSettingsComponent extends WidgetSettingsComponent {

  gatewayConfigSingleDeviceWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.gatewayConfigSingleDeviceWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      gatewayTitle: 'Gateway configuration (Single device)',
      readOnly: false
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.gatewayConfigSingleDeviceWidgetSettingsForm = this.fb.group({
      gatewayTitle: [settings.gatewayTitle, []],
      readOnly: [settings.readOnly, []]
    });
  }
}
