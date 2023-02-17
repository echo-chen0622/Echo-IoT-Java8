import {ChangeDetectorRef, Component, Inject} from '@angular/core';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {Customer} from '@shared/models/customer.model';
import {ActionNotificationShow} from '@app/core/notification/notification.actions';
import {TranslateService} from '@ngx-translate/core';
import {ContactBasedComponent} from '../../components/entity/contact-based.component';
import {EntityTableConfig} from '@home/models/entity/entities-table-config.models';
import {isDefinedAndNotNull} from '@core/utils';
import {getCurrentAuthState} from '@core/auth/auth.selectors';
import {AuthState} from '@core/auth/auth.models';

@Component({
  selector: 'tb-customer',
  templateUrl: './customer.component.html',
  styleUrls: ['./customer.component.scss']
})
export class CustomerComponent extends ContactBasedComponent<Customer> {

  isPublic = false;

  authState: AuthState = getCurrentAuthState(this.store);

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Inject('entity') protected entityValue: Customer,
              @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<Customer>,
              protected fb: FormBuilder,
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

  buildEntityForm(entity: Customer): FormGroup {
    return this.fb.group(
      {
        title: [entity ? entity.title : '', [Validators.required, Validators.maxLength(255)]],
        additionalInfo: this.fb.group(
          {
            description: [entity && entity.additionalInfo ? entity.additionalInfo.description : ''],
            homeDashboardId: [entity && entity.additionalInfo ? entity.additionalInfo.homeDashboardId : null],
            homeDashboardHideToolbar: [entity && entity.additionalInfo &&
            isDefinedAndNotNull(entity.additionalInfo.homeDashboardHideToolbar) ? entity.additionalInfo.homeDashboardHideToolbar : true]
          }
        )
      }
    );
  }

  updateEntityForm(entity: Customer) {
    this.isPublic = entity.additionalInfo && entity.additionalInfo.isPublic;
    this.entityForm.patchValue({title: entity.title});
    this.entityForm.patchValue({additionalInfo: {description: entity.additionalInfo ? entity.additionalInfo.description : ''}});
    this.entityForm.patchValue({additionalInfo:
        {homeDashboardId: entity.additionalInfo ? entity.additionalInfo.homeDashboardId : null}});
    this.entityForm.patchValue({additionalInfo:
        {homeDashboardHideToolbar: entity.additionalInfo &&
          isDefinedAndNotNull(entity.additionalInfo.homeDashboardHideToolbar) ? entity.additionalInfo.homeDashboardHideToolbar : true}});
  }

  onCustomerIdCopied(event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('customer.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

  edgesSupportEnabled() {
    return this.authState.edgesSupportEnabled;
  }
}
