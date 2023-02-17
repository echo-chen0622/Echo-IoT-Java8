import { ChangeDetectorRef, Component, Inject, Input, Optional } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { createTenantProfileConfiguration, TenantProfile, TenantProfileType } from '@shared/models/tenant.model';
import { ActionNotificationShow } from '@app/core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { EntityComponent } from '../entity/entity.component';
import { guid } from '@core/utils';

@Component({
  selector: 'tb-tenant-profile',
  templateUrl: './tenant-profile.component.html',
  styleUrls: ['./tenant-profile.component.scss']
})
export class TenantProfileComponent extends EntityComponent<TenantProfile> {

  @Input()
  standalone = false;

  constructor(protected store: Store<AppState>,
              protected translate: TranslateService,
              @Optional() @Inject('entity') protected entityValue: TenantProfile,
              @Optional() @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<TenantProfile>,
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

  buildForm(entity: TenantProfile): FormGroup {
    const mainQueue = [
      {
        id: guid(),
        consumerPerPartition: true,
        name: 'Main',
        packProcessingTimeout: 2000,
        partitions: 10,
        pollInterval: 25,
        processingStrategy: {
          failurePercentage: 0,
          maxPauseBetweenRetries: 3,
          pauseBetweenRetries: 3,
          retries: 3,
          type: 'SKIP_ALL_FAILURES'
        },
        submitStrategy: {
          batchSize: 1000,
          type: 'BURST'
        },
        topic: 'tb_rule_engine.main',
        additionalInfo: {
          description: ''
        }
      }
    ];
    const formGroup = this.fb.group(
      {
        name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
        isolatedTbRuleEngine: [entity ? entity.isolatedTbRuleEngine : false, []],
        profileData: this.fb.group({
          configuration: [entity && !this.isAdd ? entity?.profileData.configuration
            : createTenantProfileConfiguration(TenantProfileType.DEFAULT), []],
          queueConfiguration: [entity && !this.isAdd ? entity?.profileData.queueConfiguration : null, []]
        }),
        description: [entity ? entity.description : '', []],
      }
    );
    formGroup.get('isolatedTbRuleEngine').valueChanges.subscribe((value) => {
      if (value) {
        formGroup.get('profileData').patchValue({
            queueConfiguration: mainQueue
          }, {emitEvent: false});
      } else {
        formGroup.get('profileData').patchValue({
            queueConfiguration: null
          }, {emitEvent: false});
      }
    });
    return formGroup;
  }

  updateForm(entity: TenantProfile) {
    this.entityForm.patchValue({name: entity.name}, {emitEvent: false});
    this.entityForm.patchValue({isolatedTbRuleEngine: entity.isolatedTbRuleEngine}, {emitEvent: false});
    this.entityForm.get('profileData').patchValue({
      configuration: !this.isAdd ? entity.profileData?.configuration : createTenantProfileConfiguration(TenantProfileType.DEFAULT)
    }, {emitEvent: false});
    this.entityForm.get('profileData').patchValue({queueConfiguration: entity.profileData?.queueConfiguration}, {emitEvent: false});
    this.entityForm.patchValue({description: entity.description}, {emitEvent: false});
  }

  updateFormState() {
    if (this.entityForm) {
      if (this.isEditValue) {
        this.entityForm.enable({emitEvent: false});
        if (!this.isAdd) {
          this.entityForm.get('isolatedTbRuleEngine').disable({emitEvent: false});
        }
      } else {
        this.entityForm.disable({emitEvent: false});
      }
    }
  }

  onTenantProfileIdCopied(event) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message: this.translate.instant('tenant-profile.idCopiedMessage'),
        type: 'success',
        duration: 750,
        verticalPosition: 'bottom',
        horizontalPosition: 'right'
      }));
  }

}
