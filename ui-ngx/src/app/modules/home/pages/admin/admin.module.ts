import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';

import {AdminRoutingModule} from './admin-routing.module';
import {SharedModule} from '@app/shared/shared.module';
import {MailServerComponent} from '@modules/home/pages/admin/mail-server.component';
import {GeneralSettingsComponent} from '@modules/home/pages/admin/general-settings.component';
import {SecuritySettingsComponent} from '@modules/home/pages/admin/security-settings.component';
import {HomeComponentsModule} from '@modules/home/components/home-components.module';
import {OAuth2SettingsComponent} from '@modules/home/pages/admin/oauth2-settings.component';
import {SmsProviderComponent} from '@home/pages/admin/sms-provider.component';
import {SendTestSmsDialogComponent} from '@home/pages/admin/send-test-sms-dialog.component';
import {HomeSettingsComponent} from '@home/pages/admin/home-settings.component';
import {ResourcesLibraryComponent} from '@home/pages/admin/resource/resources-library.component';
import {QueueComponent} from '@home/pages/admin/queue/queue.component';
import {RepositoryAdminSettingsComponent} from '@home/pages/admin/repository-admin-settings.component';
import {AutoCommitAdminSettingsComponent} from '@home/pages/admin/auto-commit-admin-settings.component';
import {TwoFactorAuthSettingsComponent} from '@home/pages/admin/two-factor-auth-settings.component';

@NgModule({
  declarations:
    [
      GeneralSettingsComponent,
      MailServerComponent,
      SmsProviderComponent,
      SendTestSmsDialogComponent,
      SecuritySettingsComponent,
      OAuth2SettingsComponent,
      HomeSettingsComponent,
      ResourcesLibraryComponent,
      QueueComponent,
      RepositoryAdminSettingsComponent,
      AutoCommitAdminSettingsComponent,
      TwoFactorAuthSettingsComponent
    ],
  imports: [
    CommonModule,
    SharedModule,
    HomeComponentsModule,
    AdminRoutingModule
  ]
})
export class AdminModule { }
