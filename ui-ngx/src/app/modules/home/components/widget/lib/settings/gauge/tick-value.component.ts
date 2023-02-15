///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { ValueSourceProperty } from '@home/components/widget/lib/settings/common/value-source.component';
import { Component, EventEmitter, forwardRef, Input, OnInit, Output } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR } from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { isNumber } from '@core/utils';
import { IAliasController } from '@core/api/widget-api.models';

@Component({
  selector: 'tb-tick-value',
  templateUrl: './tick-value.component.html',
  styleUrls: ['./tick-value.component.scss', './../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TickValueComponent),
      multi: true
    }
  ]
})
export class TickValueComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  expanded = false;

  @Input()
  aliasController: IAliasController;

  @Output()
  removeTickValue = new EventEmitter();

  private modelValue: ValueSourceProperty;

  private propagateChange = null;

  public tickValueFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.tickValueFormGroup = this.fb.group({
      tickValue: [null, []]
    });
    this.tickValueFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.tickValueFormGroup.disable({emitEvent: false});
    } else {
      this.tickValueFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: ValueSourceProperty): void {
    this.modelValue = value;
    this.tickValueFormGroup.patchValue(
      {tickValue: value}, {emitEvent: false}
    );
  }

  tickValueText(): string {
    const value: ValueSourceProperty = this.tickValueFormGroup.get('tickValue').value;
    return this.valueSourcePropertyText(value);
  }

  private valueSourcePropertyText(source?: ValueSourceProperty): string {
    if (source) {
      if (source.valueSource === 'predefinedValue') {
        return `${isNumber(source.value) ? source.value : 0}`;
      } else if (source.valueSource === 'entityAttribute') {
        const alias = source.entityAlias || 'Undefined';
        const key = source.attribute || 'Undefined';
        return `${alias}.${key}`;
      }
    }
    return 'Undefined';
  }

  private updateModel() {
    const value: ValueSourceProperty = this.tickValueFormGroup.get('tickValue').value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }
}
