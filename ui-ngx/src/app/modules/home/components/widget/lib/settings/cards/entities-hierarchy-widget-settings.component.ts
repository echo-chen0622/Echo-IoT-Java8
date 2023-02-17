import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-entities-hierarchy-widget-settings',
  templateUrl: './entities-hierarchy-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class EntitiesHierarchyWidgetSettingsComponent extends WidgetSettingsComponent {

  entitiesHierarchyWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.entitiesHierarchyWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      nodeRelationQueryFunction: '',
      nodeHasChildrenFunction: '',
      nodeOpenedFunction: '',
      nodeDisabledFunction: '',
      nodeIconFunction: '',
      nodeTextFunction: '',
      nodesSortFunction: '',
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.entitiesHierarchyWidgetSettingsForm = this.fb.group({
      nodeRelationQueryFunction: [settings.nodeRelationQueryFunction, []],
      nodeHasChildrenFunction: [settings.nodeHasChildrenFunction, []],
      nodeOpenedFunction: [settings.nodeOpenedFunction, []],
      nodeDisabledFunction: [settings.nodeDisabledFunction, []],
      nodeIconFunction: [settings.nodeIconFunction, []],
      nodeTextFunction: [settings.nodeTextFunction, []],
      nodesSortFunction: [settings.nodesSortFunction, []]
    });
  }
}
