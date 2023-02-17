import { Component, Inject, ViewChild } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TwoFactorAuthenticationService } from '@core/http/two-factor-authentication.service';
import {
  AccountTwoFaSettings,
  TwoFactorAuthAccountConfig,
  TwoFactorAuthProviderType
} from '@shared/models/two-factor-auth.models';
import { MatStepper } from '@angular/material/stepper';

export interface EmailAuthDialogData {
  email: string;
}

@Component({
  selector: 'tb-email-auth-dialog',
  templateUrl: './email-auth-dialog.component.html',
  styleUrls: ['./authentication-dialog.component.scss']
})
export class EmailAuthDialogComponent extends DialogComponent<EmailAuthDialogComponent> {

  private authAccountConfig: TwoFactorAuthAccountConfig;
  private config: AccountTwoFaSettings;

  emailConfigForm: FormGroup;
  emailVerificationForm: FormGroup;

  @ViewChild('stepper', {static: false}) stepper: MatStepper;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private twoFaService: TwoFactorAuthenticationService,
              @Inject(MAT_DIALOG_DATA) public data: EmailAuthDialogData,
              public dialogRef: MatDialogRef<EmailAuthDialogComponent>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);

    this.emailConfigForm = this.fb.group({
      email: [this.data.email, [Validators.required, Validators.email]]
    });

    this.emailVerificationForm = this.fb.group({
      verificationCode: ['', [
        Validators.required,
        Validators.minLength(6),
        Validators.maxLength(6),
        Validators.pattern(/^\d*$/)
      ]]
    });
  }

  nextStep() {
    switch (this.stepper.selectedIndex) {
      case 0:
        if (this.emailConfigForm.valid) {
          this.authAccountConfig = {
            providerType: TwoFactorAuthProviderType.EMAIL,
            useByDefault: true,
            email: this.emailConfigForm.get('email').value as string
          };
          this.twoFaService.submitTwoFaAccountConfig(this.authAccountConfig).subscribe(() => {
            this.stepper.next();
          });
        } else {
          this.showFormErrors(this.emailConfigForm);
        }
        break;
      case 1:
        if (this.emailVerificationForm.valid) {
          this.twoFaService.verifyAndSaveTwoFaAccountConfig(this.authAccountConfig,
            this.emailVerificationForm.get('verificationCode').value).subscribe((config) => {
              this.config = config;
              this.stepper.next();
            });
        } else {
          this.showFormErrors(this.emailVerificationForm);
        }
        break;
    }
  }

  closeDialog() {
    return this.dialogRef.close(this.config);
  }

  private showFormErrors(form: FormGroup) {
    Object.keys(form.controls).forEach(field => {
      const control = form.get(field);
      control.markAsTouched({onlySelf: true});
    });
  }
}
