import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';

import {LoginRoutingModule} from './login-routing.module';
import {LoginComponent} from './pages/login/login.component';
import {SharedModule} from '@app/shared/shared.module';
import {ResetPasswordRequestComponent} from '@modules/login/pages/login/reset-password-request.component';
import {ResetPasswordComponent} from '@modules/login/pages/login/reset-password.component';
import {CreatePasswordComponent} from '@modules/login/pages/login/create-password.component';
import {TwoFactorAuthLoginComponent} from '@modules/login/pages/login/two-factor-auth-login.component';

@NgModule({
  declarations: [
    LoginComponent,
    ResetPasswordRequestComponent,
    ResetPasswordComponent,
    CreatePasswordComponent,
    TwoFactorAuthLoginComponent
  ],
  imports: [
    CommonModule,
    SharedModule,
    LoginRoutingModule
  ]
})
export class LoginModule { }
