import {Component} from '@angular/core';
import {WidgetSettings, WidgetSettingsComponent} from '@shared/models/widget.models';
import {FormBuilder, FormGroup} from '@angular/forms';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';

@Component({
  selector: 'tb-edge-quick-overview-widget-settings',
  templateUrl: './edge-quick-overview-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class EdgeQuickOverviewWidgetSettingsComponent extends WidgetSettingsComponent {

  edgeQuickOverviewWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.edgeQuickOverviewWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      enableDefaultTitle: true
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.edgeQuickOverviewWidgetSettingsForm = this.fb.group({
      enableDefaultTitle: [settings.enableDefaultTitle, []]
    });
  }
}
