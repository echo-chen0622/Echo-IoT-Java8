import {Component} from '@angular/core';
import {WidgetSettings, WidgetSettingsComponent} from '@shared/models/widget.models';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';

@Component({
  selector: 'tb-flot-pie-widget-settings',
  templateUrl: './flot-pie-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class FlotPieWidgetSettingsComponent extends WidgetSettingsComponent {

  flotPieWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.flotPieWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      radius: 1,
      innerRadius: 0,
      tilt: 1,
      stroke: {
        color: '',
        width: 0
      },
      showLabels: false,
      showTooltip: true,
      animatedPie: false
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {

    this.flotPieWidgetSettingsForm = this.fb.group({

      // Common pie settings

      radius: [settings.radius, [Validators.min(0)]],
      innerRadius: [settings.innerRadius, [Validators.min(0)]],
      tilt: [settings.tilt, [Validators.min(0)]],

      // Stroke settings

      stroke: this.fb.group({
        color: [settings.stroke?.color, []],
        width: [settings.stroke?.width, [Validators.min(0)]]
      }),

      showLabels: [settings.showLabels, []],
      showTooltip: [settings.showTooltip, []],

      animatedPie: [settings.animatedPie, []],
    });
  }
}
