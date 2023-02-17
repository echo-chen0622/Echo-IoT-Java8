import {Component} from '@angular/core';
import {WidgetSettings, WidgetSettingsComponent} from '@shared/models/widget.models';
import {FormBuilder, FormGroup} from '@angular/forms';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';

@Component({
  selector: 'tb-gateway-config-widget-settings',
  templateUrl: './gateway-config-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class GatewayConfigWidgetSettingsComponent extends WidgetSettingsComponent {

  gatewayConfigWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.gatewayConfigWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      widgetTitle: 'Gateway Configuration',
      archiveFileName: 'gatewayConfiguration',
      gatewayType: 'Gateway',
      successfulSave: '',
      gatewayNameExists: ''
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.gatewayConfigWidgetSettingsForm = this.fb.group({
      widgetTitle: [settings.widgetTitle, []],
      archiveFileName: [settings.archiveFileName, []],
      gatewayType: [settings.gatewayType, []],
      successfulSave: [settings.successfulSave, []],
      gatewayNameExists: [settings.gatewayNameExists, []]
    });
  }
}
