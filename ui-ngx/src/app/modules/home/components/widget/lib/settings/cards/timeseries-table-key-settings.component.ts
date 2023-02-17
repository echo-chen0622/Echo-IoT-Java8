import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-timeseries-table-key-settings',
  templateUrl: './timeseries-table-key-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class TimeseriesTableKeySettingsComponent extends WidgetSettingsComponent {

  timeseriesTableKeySettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.timeseriesTableKeySettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      useCellStyleFunction: false,
      cellStyleFunction: '',
      useCellContentFunction: false,
      cellContentFunction: ''
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.timeseriesTableKeySettingsForm = this.fb.group({
      useCellStyleFunction: [settings.useCellStyleFunction, []],
      cellStyleFunction: [settings.cellStyleFunction, [Validators.required]],
      useCellContentFunction: [settings.useCellContentFunction, []],
      cellContentFunction: [settings.cellContentFunction, [Validators.required]],
    });
  }

  protected validatorTriggers(): string[] {
    return ['useCellStyleFunction', 'useCellContentFunction'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useCellStyleFunction: boolean = this.timeseriesTableKeySettingsForm.get('useCellStyleFunction').value;
    const useCellContentFunction: boolean = this.timeseriesTableKeySettingsForm.get('useCellContentFunction').value;
    if (useCellStyleFunction) {
      this.timeseriesTableKeySettingsForm.get('cellStyleFunction').enable();
    } else {
      this.timeseriesTableKeySettingsForm.get('cellStyleFunction').disable();
    }
    if (useCellContentFunction) {
      this.timeseriesTableKeySettingsForm.get('cellContentFunction').enable();
    } else {
      this.timeseriesTableKeySettingsForm.get('cellContentFunction').disable();
    }
    this.timeseriesTableKeySettingsForm.get('cellStyleFunction').updateValueAndValidity({emitEvent});
    this.timeseriesTableKeySettingsForm.get('cellContentFunction').updateValueAndValidity({emitEvent});
  }

}
