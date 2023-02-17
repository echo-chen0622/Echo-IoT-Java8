import { Component, OnInit } from '@angular/core';
import { UserService } from '@core/http/user.service';
import { AuthUser, User } from '@shared/models/user.model';
import { Authority } from '@shared/models/authority.enum';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { ActionAuthUpdateUserDetails } from '@core/auth/auth.actions';
import { environment as env } from '@env/environment';
import { TranslateService } from '@ngx-translate/core';
import { ActionSettingsChangeLanguage } from '@core/settings/settings.actions';
import { ActivatedRoute } from '@angular/router';
import { isDefinedAndNotNull } from '@core/utils';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';

@Component({
  selector: 'tb-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent extends PageComponent implements OnInit, HasConfirmForm {

  authorities = Authority;
  profile: FormGroup;
  user: User;
  languageList = env.supportedLangs;
  private readonly authUser: AuthUser;

  constructor(protected store: Store<AppState>,
              private route: ActivatedRoute,
              private userService: UserService,
              private translate: TranslateService,
              public fb: FormBuilder) {
    super(store);
    this.authUser = getCurrentAuthUser(this.store);
  }

  ngOnInit() {
    this.buildProfileForm();
    this.userLoaded(this.route.snapshot.data.user);
  }

  private buildProfileForm() {
    this.profile = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      firstName: [''],
      lastName: [''],
      language: [''],
      homeDashboardId: [null],
      homeDashboardHideToolbar: [true]
    });
  }

  save(): void {
    this.user = {...this.user, ...this.profile.value};
    if (!this.user.additionalInfo) {
      this.user.additionalInfo = {};
    }
    this.user.additionalInfo.lang = this.profile.get('language').value;
    this.user.additionalInfo.homeDashboardId = this.profile.get('homeDashboardId').value;
    this.user.additionalInfo.homeDashboardHideToolbar = this.profile.get('homeDashboardHideToolbar').value;
    this.userService.saveUser(this.user).subscribe(
      (user) => {
        this.userLoaded(user);
        this.store.dispatch(new ActionAuthUpdateUserDetails({ userDetails: {
            additionalInfo: {...user.additionalInfo},
            authority: user.authority,
            createdTime: user.createdTime,
            tenantId: user.tenantId,
            customerId: user.customerId,
            email: user.email,
            firstName: user.firstName,
            id: user.id,
            lastName: user.lastName,
          } }));
        this.store.dispatch(new ActionSettingsChangeLanguage({ userLang: user.additionalInfo.lang }));
      }
    );
  }

  private userLoaded(user: User) {
    this.user = user;
    this.profile.reset(user);
    let lang;
    let homeDashboardId;
    let homeDashboardHideToolbar = true;
    if (user.additionalInfo) {
      if (user.additionalInfo.lang) {
        lang = user.additionalInfo.lang;
      }
      homeDashboardId = user.additionalInfo.homeDashboardId;
      if (isDefinedAndNotNull(user.additionalInfo.homeDashboardHideToolbar)) {
        homeDashboardHideToolbar = user.additionalInfo.homeDashboardHideToolbar;
      }
    }
    if (!lang) {
      lang = this.translate.currentLang;
    }
    this.profile.get('language').setValue(lang);
    this.profile.get('homeDashboardId').setValue(homeDashboardId);
    this.profile.get('homeDashboardHideToolbar').setValue(homeDashboardHideToolbar);
  }

  confirmForm(): FormGroup {
    return this.profile;
  }

  isSysAdmin(): boolean {
    return this.authUser.authority === Authority.SYS_ADMIN;
  }
}
