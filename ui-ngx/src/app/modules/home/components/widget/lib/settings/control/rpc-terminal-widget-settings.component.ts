import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-rpc-terminal-widget-settings',
  templateUrl: './rpc-terminal-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class RpcTerminalWidgetSettingsComponent extends WidgetSettingsComponent {

  rpcTerminalWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.rpcTerminalWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      requestTimeout: 500,
      requestPersistent: false,
      persistentPollingInterval: 5000
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.rpcTerminalWidgetSettingsForm = this.fb.group({
      // RPC settings

      requestTimeout: [settings.requestTimeout, [Validators.min(0), Validators.required]],

      // --> Persistent RPC settings

      requestPersistent: [settings.requestPersistent, []],
      persistentPollingInterval: [settings.persistentPollingInterval, [Validators.min(1000)]]
    });
  }

  protected validatorTriggers(): string[] {
    return ['requestPersistent'];
  }

  protected updateValidators(emitEvent: boolean): void {
    const requestPersistent: boolean = this.rpcTerminalWidgetSettingsForm.get('requestPersistent').value;
    if (requestPersistent) {
      this.rpcTerminalWidgetSettingsForm.get('persistentPollingInterval').enable({emitEvent});
    } else {
      this.rpcTerminalWidgetSettingsForm.get('persistentPollingInterval').disable({emitEvent});
    }
    this.rpcTerminalWidgetSettingsForm.get('persistentPollingInterval').updateValueAndValidity({emitEvent: false});
  }
}
