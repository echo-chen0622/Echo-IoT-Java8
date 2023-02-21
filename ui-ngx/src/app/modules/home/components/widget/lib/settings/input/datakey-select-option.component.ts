import {Component, EventEmitter, forwardRef, Input, OnInit, Output} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALUE_ACCESSOR,
  Validators
} from '@angular/forms';
import {PageComponent} from '@shared/components/page.component';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {TranslateService} from '@ngx-translate/core';
import {DomSanitizer, SafeHtml} from '@angular/platform-browser';

export interface DataKeySelectOption {
  value: string;
  label?: string;
}

export function dataKeySelectOptionValidator(control: AbstractControl) {
    const selectOption: DataKeySelectOption = control.value;
    if (!selectOption || !selectOption.value) {
      return {
        dataKeySelectOption: true
      };
    }
    return null;
}

@Component({
  selector: 'tb-datakey-select-option',
  templateUrl: './datakey-select-option.component.html',
  styleUrls: ['./datakey-select-option.component.scss', './../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DataKeySelectOptionComponent),
      multi: true
    }
  ]
})
export class DataKeySelectOptionComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  expanded = false;

  @Output()
  removeSelectOption = new EventEmitter();

  private modelValue: DataKeySelectOption;

  private propagateChange = null;

  public selectOptionFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private domSanitizer: DomSanitizer,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.selectOptionFormGroup = this.fb.group({
      value: [null, [Validators.required]],
      label: [null, []]
    });
    this.selectOptionFormGroup.valueChanges.subscribe(() => {
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
      this.selectOptionFormGroup.disable({emitEvent: false});
    } else {
      this.selectOptionFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: DataKeySelectOption): void {
    this.modelValue = value;
    this.selectOptionFormGroup.patchValue(
      value, {emitEvent: false}
    );
  }

  selectOptionHtml(): SafeHtml {
    const selectOption: DataKeySelectOption = this.selectOptionFormGroup.value;
    const value = selectOption?.value || 'Undefined';
    const label = selectOption?.label || '';
    return this.domSanitizer.bypassSecurityTrustHtml(`${value} ${label ? '(<small>' + label + '</small>)' : ''}`);
  }

  private updateModel() {
    const value: DataKeySelectOption = this.selectOptionFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }
}
