import { Component, OnInit, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormGroup } from '@angular/forms';
import { RepositorySettingsComponent } from '@home/components/vc/repository-settings.component';

@Component({
  selector: 'tb-repository-admin-settings',
  templateUrl: './repository-admin-settings.component.html',
  styleUrls: []
})
export class RepositoryAdminSettingsComponent extends PageComponent implements OnInit, HasConfirmForm {

  @ViewChild('repositorySettingsComponent') repositorySettingsComponent: RepositorySettingsComponent;

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit() {
  }

  confirmForm(): FormGroup {
    return this.repositorySettingsComponent?.repositorySettingsForm;
  }
}
