import {Component} from '@angular/core';
import {WidgetSettings, WidgetSettingsComponent} from '@shared/models/widget.models';
import {FormBuilder, FormGroup} from '@angular/forms';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';

@Component({
  selector: 'tb-chart-widget-settings',
  templateUrl: './chart-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class ChartWidgetSettingsComponent extends WidgetSettingsComponent {

  chartWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.chartWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      showTooltip: true
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.chartWidgetSettingsForm = this.fb.group({
      showTooltip: [settings.showTooltip, []]
    });
  }
}
