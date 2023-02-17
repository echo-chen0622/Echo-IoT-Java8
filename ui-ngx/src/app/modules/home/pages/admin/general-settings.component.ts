import {Component, OnInit} from '@angular/core';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {PageComponent} from '@shared/components/page.component';
import {Router} from '@angular/router';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {AdminSettings, GeneralSettings} from '@shared/models/settings.models';
import {AdminService} from '@core/http/admin.service';
import {HasConfirmForm} from '@core/guards/confirm-on-exit.guard';

@Component({
  selector: 'tb-general-settings',
  templateUrl: './general-settings.component.html',
  styleUrls: ['./general-settings.component.scss', './settings-card.scss']
})
export class GeneralSettingsComponent extends PageComponent implements OnInit, HasConfirmForm {

  generalSettings: FormGroup;
  adminSettings: AdminSettings<GeneralSettings>;

  constructor(protected store: Store<AppState>,
              private router: Router,
              private adminService: AdminService,
              public fb: FormBuilder) {
    super(store);
  }

  ngOnInit() {
    this.buildGeneralServerSettingsForm();
    this.adminService.getAdminSettings<GeneralSettings>('general').subscribe(
      (adminSettings) => {
        this.adminSettings = adminSettings;
        this.generalSettings.reset(this.adminSettings.jsonValue);
      }
    );
  }

  buildGeneralServerSettingsForm() {
    this.generalSettings = this.fb.group({
      baseUrl: ['', [Validators.required]],
      prohibitDifferentUrl: ['',[]]
    });
  }

  save(): void {
    this.adminSettings.jsonValue = {...this.adminSettings.jsonValue, ...this.generalSettings.value};
    this.adminService.saveAdminSettings(this.adminSettings).subscribe(
      (adminSettings) => {
        this.adminSettings = adminSettings;
        this.generalSettings.reset(this.adminSettings.jsonValue);
      }
    );
  }

  confirmForm(): FormGroup {
    return this.generalSettings;
  }

}
