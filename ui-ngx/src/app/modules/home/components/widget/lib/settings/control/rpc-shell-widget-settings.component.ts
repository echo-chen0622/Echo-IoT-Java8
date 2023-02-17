import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-rpc-shell-widget-settings',
  templateUrl: './rpc-shell-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class RpcShellWidgetSettingsComponent extends WidgetSettingsComponent {

  rpcShellWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.rpcShellWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      requestTimeout: 500
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.rpcShellWidgetSettingsForm = this.fb.group({
      // RPC settings
      requestTimeout: [settings.requestTimeout, [Validators.min(0), Validators.required]]
    });
  }
}
