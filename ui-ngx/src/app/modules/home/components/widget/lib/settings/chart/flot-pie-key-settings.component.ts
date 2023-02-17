import {Component} from '@angular/core';
import {WidgetSettings, WidgetSettingsComponent} from '@shared/models/widget.models';
import {FormBuilder, FormGroup} from '@angular/forms';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';

@Component({
  selector: 'tb-flot-pie-key-settings',
  templateUrl: './flot-pie-key-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class FlotPieKeySettingsComponent extends WidgetSettingsComponent {

  flotPieKeySettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.flotPieKeySettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      hideDataByDefault: false,
      disableDataHiding: false,
      removeFromLegend: false
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {

    this.flotPieKeySettingsForm = this.fb.group({

      // Common settings

      hideDataByDefault: [settings.hideDataByDefault, []],
      disableDataHiding: [settings.disableDataHiding, []],
      removeFromLegend: [settings.removeFromLegend, []]
    });
  }
}
