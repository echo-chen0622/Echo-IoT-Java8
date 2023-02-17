import { Component, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AdminSettings, SmsProviderConfiguration } from '@shared/models/settings.models';
import { AdminService } from '@core/http/admin.service';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { MatDialog } from '@angular/material/dialog';
import { SendTestSmsDialogComponent, SendTestSmsDialogData } from '@home/pages/admin/send-test-sms-dialog.component';

@Component({
  selector: 'tb-sms-provider',
  templateUrl: './sms-provider.component.html',
  styleUrls: ['./sms-provider.component.scss', './settings-card.scss']
})
export class SmsProviderComponent extends PageComponent implements OnInit, HasConfirmForm {

  smsProvider: FormGroup;
  adminSettings: AdminSettings<SmsProviderConfiguration>;

  constructor(protected store: Store<AppState>,
              private router: Router,
              private adminService: AdminService,
              private dialog: MatDialog,
              public fb: FormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.buildSmsProviderForm();
    this.adminService.getAdminSettings<SmsProviderConfiguration>('sms', {ignoreErrors: true}).subscribe(
      (adminSettings) => {
        this.adminSettings = adminSettings;
        this.smsProvider.reset({configuration: this.adminSettings.jsonValue});
      },
      () => {
        this.adminSettings = {
          key: 'sms',
          jsonValue: null
        };
        this.smsProvider.reset({configuration: this.adminSettings.jsonValue});
      }
    );
  }

  buildSmsProviderForm() {
    this.smsProvider = this.fb.group({
      configuration: [null, [Validators.required]]
    });
    this.registerDisableOnLoadFormControl(this.smsProvider.get('configuration'));
  }

  sendTestSms(): void {
    this.dialog.open<SendTestSmsDialogComponent, SendTestSmsDialogData>(SendTestSmsDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        smsProviderConfiguration: this.smsProvider.value.configuration
      }
    });
  }

  save(): void {
    this.adminSettings.jsonValue = this.smsProvider.value.configuration;
    this.adminService.saveAdminSettings(this.adminSettings).subscribe(
      (adminSettings) => {
        this.adminSettings = adminSettings;
        this.smsProvider.reset({configuration: this.adminSettings.jsonValue});
      }
    );
  }

  confirmForm(): FormGroup {
    return this.smsProvider;
  }

}
