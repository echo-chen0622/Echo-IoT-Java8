import { Component, OnInit } from '@angular/core';
import { AuthService } from '@core/auth/auth.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { FormBuilder, Validators } from '@angular/forms';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'tb-reset-password-request',
  templateUrl: './reset-password-request.component.html',
  styleUrls: ['./reset-password-request.component.scss']
})
export class ResetPasswordRequestComponent extends PageComponent implements OnInit {

  clicked: boolean = false;

  requestPasswordRequest = this.fb.group({
    email: ['', [Validators.email, Validators.required]]
  }, {updateOn: 'submit'});

  constructor(protected store: Store<AppState>,
              private authService: AuthService,
              private translate: TranslateService,
              public fb: FormBuilder) {
    super(store);
  }

  ngOnInit() {
  }

  disableInputs() {
    this.requestPasswordRequest.disable();
    this.clicked = true;
  }

  sendResetPasswordLink() {
    if (this.requestPasswordRequest.valid) {
      this.disableInputs();
      this.authService.sendResetPasswordLink(this.requestPasswordRequest.get('email').value).subscribe(
        () => {
          this.store.dispatch(new ActionNotificationShow({
            message: this.translate.instant('login.password-link-sent-message'),
            type: 'success'
          }));
        }
      );
    }
  }

}
