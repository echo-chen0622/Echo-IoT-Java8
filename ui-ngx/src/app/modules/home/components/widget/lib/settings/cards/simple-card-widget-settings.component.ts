///
/// Copyright © 2016-2023 The Echoiot Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-simple-card-widget-settings',
  templateUrl: './simple-card-widget-settings.component.html',
  styleUrls: []
})
export class SimpleCardWidgetSettingsComponent extends WidgetSettingsComponent {

  simpleCardWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.simpleCardWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      labelPosition: 'left'
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.simpleCardWidgetSettingsForm = this.fb.group({
      labelPosition: [settings.labelPosition, []]
    });
  }
}
