import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-timeseries-table-latest-key-settings',
  templateUrl: './timeseries-table-latest-key-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class TimeseriesTableLatestKeySettingsComponent extends WidgetSettingsComponent {

  timeseriesTableLatestKeySettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.timeseriesTableLatestKeySettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      show: true,
      useCellStyleFunction: false,
      cellStyleFunction: '',
      useCellContentFunction: false,
      cellContentFunction: ''
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.timeseriesTableLatestKeySettingsForm = this.fb.group({
      show: [settings.show, []],
      order: [settings.order, []],
      useCellStyleFunction: [settings.useCellStyleFunction, []],
      cellStyleFunction: [settings.cellStyleFunction, [Validators.required]],
      useCellContentFunction: [settings.useCellContentFunction, []],
      cellContentFunction: [settings.cellContentFunction, [Validators.required]],
    });
  }

  protected validatorTriggers(): string[] {
    return ['show', 'useCellStyleFunction', 'useCellContentFunction'];
  }

  protected updateValidators(emitEvent: boolean) {
    const show: boolean = this.timeseriesTableLatestKeySettingsForm.get('show').value;
    if (show) {
      this.timeseriesTableLatestKeySettingsForm.get('order').enable();
      this.timeseriesTableLatestKeySettingsForm.get('useCellStyleFunction').enable({emitEvent: false});
      this.timeseriesTableLatestKeySettingsForm.get('useCellContentFunction').enable({emitEvent: false});
      const useCellStyleFunction: boolean = this.timeseriesTableLatestKeySettingsForm.get('useCellStyleFunction').value;
      const useCellContentFunction: boolean = this.timeseriesTableLatestKeySettingsForm.get('useCellContentFunction').value;
      if (useCellStyleFunction) {
        this.timeseriesTableLatestKeySettingsForm.get('cellStyleFunction').enable();
      } else {
        this.timeseriesTableLatestKeySettingsForm.get('cellStyleFunction').disable();
      }
      if (useCellContentFunction) {
        this.timeseriesTableLatestKeySettingsForm.get('cellContentFunction').enable();
      } else {
        this.timeseriesTableLatestKeySettingsForm.get('cellContentFunction').disable();
      }
    } else {
      this.timeseriesTableLatestKeySettingsForm.get('order').disable();
      this.timeseriesTableLatestKeySettingsForm.get('useCellStyleFunction').disable({emitEvent: false});
      this.timeseriesTableLatestKeySettingsForm.get('cellStyleFunction').disable();
      this.timeseriesTableLatestKeySettingsForm.get('useCellContentFunction').disable({emitEvent: false});
      this.timeseriesTableLatestKeySettingsForm.get('cellContentFunction').disable();
    }
    this.timeseriesTableLatestKeySettingsForm.get('order').updateValueAndValidity({emitEvent});
    this.timeseriesTableLatestKeySettingsForm.get('cellStyleFunction').updateValueAndValidity({emitEvent});
    this.timeseriesTableLatestKeySettingsForm.get('cellContentFunction').updateValueAndValidity({emitEvent});
  }

}
