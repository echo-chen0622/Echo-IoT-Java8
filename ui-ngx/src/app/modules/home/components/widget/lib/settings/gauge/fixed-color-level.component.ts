import {ValueSourceProperty} from '@home/components/widget/lib/settings/common/value-source.component';
import {Component, EventEmitter, forwardRef, Input, OnInit, Output} from '@angular/core';
import {
    AbstractControl,
    ControlValueAccessor,
    FormBuilder,
    FormGroup,
    NG_VALUE_ACCESSOR,
    ValidationErrors,
    Validators
} from '@angular/forms';
import {PageComponent} from '@shared/components/page.component';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {TranslateService} from '@ngx-translate/core';
import {isNumber} from '@core/utils';
import {IAliasController} from '@core/api/widget-api.models';

export interface FixedColorLevel {
  from?: ValueSourceProperty;
  to?: ValueSourceProperty;
  color: string;
}

export function fixedColorLevelValidator(control: AbstractControl): ValidationErrors | null {
  const fixedColorLevel: FixedColorLevel = control.value;
  if (!fixedColorLevel || !fixedColorLevel.color) {
    return {
      fixedColorLevel: true
    };
  }
  return null;
}

@Component({
  selector: 'tb-fixed-color-level',
  templateUrl: './fixed-color-level.component.html',
  styleUrls: ['./fixed-color-level.component.scss', './../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FixedColorLevelComponent),
      multi: true
    }
  ]
})
export class FixedColorLevelComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  expanded = false;

  @Input()
  aliasController: IAliasController;

  @Output()
  removeFixedColorLevel = new EventEmitter();

  private modelValue: FixedColorLevel;

  private propagateChange = null;

  public fixedColorLevelFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.fixedColorLevelFormGroup = this.fb.group({
      from: [null, []],
      to: [null, []],
      color: [null, [Validators.required]]
    });
    this.fixedColorLevelFormGroup.valueChanges.subscribe(() => {
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
      this.fixedColorLevelFormGroup.disable({emitEvent: false});
    } else {
      this.fixedColorLevelFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: FixedColorLevel): void {
    this.modelValue = value;
    this.fixedColorLevelFormGroup.patchValue(
      value, {emitEvent: false}
    );
  }

  fixedColorLevelRangeText(): string {
    const value: FixedColorLevel = this.fixedColorLevelFormGroup.value;
    const from = this.valueSourcePropertyText(value?.from);
    const to = this.valueSourcePropertyText(value?.to);
    return `${from} - ${to}`;
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
    const value: FixedColorLevel = this.fixedColorLevelFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }
}
