import {Component} from '@angular/core';
import {WidgetSettings, WidgetSettingsComponent} from '@shared/models/widget.models';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';

@Component({
  selector: 'tb-knob-control-widget-settings',
  templateUrl: './knob-control-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class KnobControlWidgetSettingsComponent extends WidgetSettingsComponent {

  knobControlWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.knobControlWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      title: '',
      minValue: 0,
      maxValue: 100,
      initialValue: 50,
      getValueMethod: 'getValue',
      setValueMethod: 'setValue',
      requestTimeout: 500,
      requestPersistent: false,
      persistentPollingInterval: 5000
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.knobControlWidgetSettingsForm = this.fb.group({

      // Common settings

      title: [settings.title, []],

      // Value settings

      initialValue: [settings.initialValue, []],
      minValue: [settings.minValue, [Validators.required]],
      maxValue: [settings.maxValue, [Validators.required]],

      getValueMethod: [settings.getValueMethod, [Validators.required]],
      setValueMethod: [settings.setValueMethod, [Validators.required]],

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
    const requestPersistent: boolean = this.knobControlWidgetSettingsForm.get('requestPersistent').value;
    if (requestPersistent) {
      this.knobControlWidgetSettingsForm.get('persistentPollingInterval').enable({emitEvent});
    } else {
      this.knobControlWidgetSettingsForm.get('persistentPollingInterval').disable({emitEvent});
    }
    this.knobControlWidgetSettingsForm.get('persistentPollingInterval').updateValueAndValidity({emitEvent: false});
  }
}
