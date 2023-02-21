import {Component} from '@angular/core';
import {WidgetSettings, WidgetSettingsComponent} from '@shared/models/widget.models';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';

@Component({
  selector: 'tb-entities-table-key-settings',
  templateUrl: './entities-table-key-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class EntitiesTableKeySettingsComponent extends WidgetSettingsComponent {

  entitiesTableKeySettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.entitiesTableKeySettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      customTitle: '',
      columnWidth: '0px',
      useCellStyleFunction: false,
      cellStyleFunction: '',
      useCellContentFunction: false,
      cellContentFunction: '',
      defaultColumnVisibility: 'visible',
      columnSelectionToDisplay: 'enabled'
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.entitiesTableKeySettingsForm = this.fb.group({
      customTitle: [settings.customTitle, []],
      columnWidth: [settings.columnWidth, []],
      useCellStyleFunction: [settings.useCellStyleFunction, []],
      cellStyleFunction: [settings.cellStyleFunction, [Validators.required]],
      useCellContentFunction: [settings.useCellContentFunction, []],
      cellContentFunction: [settings.cellContentFunction, [Validators.required]],
      defaultColumnVisibility: [settings.defaultColumnVisibility, []],
      columnSelectionToDisplay: [settings.columnSelectionToDisplay, []],
    });
  }

  protected validatorTriggers(): string[] {
    return ['useCellStyleFunction', 'useCellContentFunction'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useCellStyleFunction: boolean = this.entitiesTableKeySettingsForm.get('useCellStyleFunction').value;
    const useCellContentFunction: boolean = this.entitiesTableKeySettingsForm.get('useCellContentFunction').value;
    if (useCellStyleFunction) {
      this.entitiesTableKeySettingsForm.get('cellStyleFunction').enable();
    } else {
      this.entitiesTableKeySettingsForm.get('cellStyleFunction').disable();
    }
    if (useCellContentFunction) {
      this.entitiesTableKeySettingsForm.get('cellContentFunction').enable();
    } else {
      this.entitiesTableKeySettingsForm.get('cellContentFunction').disable();
    }
    this.entitiesTableKeySettingsForm.get('cellStyleFunction').updateValueAndValidity({emitEvent});
    this.entitiesTableKeySettingsForm.get('cellContentFunction').updateValueAndValidity({emitEvent});
  }

}
