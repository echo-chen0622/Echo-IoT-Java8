import { ChangeDetectorRef, Component, Inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { EntityComponent } from '../../components/entity/entity.component';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { WidgetsBundle } from '@shared/models/widgets-bundle.model';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';

@Component({
  selector: 'tb-widgets-bundle',
  templateUrl: './widgets-bundle.component.html',
  styleUrls: ['./widgets-bundle.component.scss']
})
export class WidgetsBundleComponent extends EntityComponent<WidgetsBundle> {

  constructor(protected store: Store<AppState>,
              @Inject('entity') protected entityValue: WidgetsBundle,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<WidgetsBundle>,
              public fb: FormBuilder,
              protected cd: ChangeDetectorRef) {
    super(store, fb, entityValue, entitiesTableConfigValue, cd);
  }

  hideDelete() {
    if (this.entitiesTableConfig) {
      return !this.entitiesTableConfig.deleteEnabled(this.entity);
    } else {
      return false;
    }
  }

  buildForm(entity: WidgetsBundle): FormGroup {
    return this.fb.group(
      {
        title: [entity ? entity.title : '', [Validators.required, Validators.maxLength(255)]],
        image: [entity ? entity.image : ''],
        description: [entity  ? entity.description : '', Validators.maxLength(255)]
      }
    );
  }

  updateForm(entity: WidgetsBundle) {
    this.entityForm.patchValue({
      title: entity.title,
      image: entity.image,
      description: entity.description
    });
  }
}
