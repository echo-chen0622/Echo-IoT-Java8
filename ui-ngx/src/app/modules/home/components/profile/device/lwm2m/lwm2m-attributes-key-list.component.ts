import {Component, forwardRef, Input, OnDestroy} from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import {Subject, Subscription} from 'rxjs';
import {
  AttributeName,
  AttributeNameTranslationMap,
  AttributesNameValue,
  AttributesNameValueMap,
  valueValidatorByAttributeName
} from './lwm2m-profile-config.models';
import {isUndefinedOrNull} from '@core/utils';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {PageComponent} from '@shared/components/page.component';
import {takeUntil} from 'rxjs/operators';

@Component({
  selector: 'tb-lwm2m-attributes-key-list',
  templateUrl: './lwm2m-attributes-key-list.component.html',
  styleUrls: ['./lwm2m-attributes-key-list.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => Lwm2mAttributesKeyListComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => Lwm2mAttributesKeyListComponent),
      multi: true,
    }
  ]
})
export class Lwm2mAttributesKeyListComponent extends PageComponent implements ControlValueAccessor, OnDestroy, OnDestroy, Validator {

  attributeNames;
  attributeNameTranslationMap = AttributeNameTranslationMap;

  @Input() disabled: boolean;

  @Input()
  isResource = false;

  attributesValueFormGroup: FormGroup;

  private propagateChange = null;
  private valueChange$: Subscription = null;
  private destroy$ = new Subject();
  private usedAttributesName: AttributeName[] = [];

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
    this.attributesValueFormGroup = this.fb.group({
      attributesValue: this.fb.array([])
    });
  }

  ngOnInit() {
    if (this.isResource) {
      this.attributeNames = Object.values(AttributeName);
    } else {
      this.attributeNames = Object.values(AttributeName)
        .filter(item => ![AttributeName.lt, AttributeName.gt, AttributeName.st].includes(item));
    }
  }

  ngOnDestroy() {
    if (this.valueChange$) {
      this.valueChange$.unsubscribe();
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.attributesValueFormGroup.disable({emitEvent: false});
    } else {
      this.attributesValueFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(keyValMap: AttributesNameValueMap): void {
    if (this.valueChange$) {
      this.valueChange$.unsubscribe();
    }
    const attributesValueControls: Array<AbstractControl> = [];
    if (keyValMap) {
      (Object.keys(keyValMap) as AttributeName[]).forEach(name => {
        attributesValueControls.push(this.createdFormGroup({name, value: keyValMap[name]}));
      });
    }
    this.attributesValueFormGroup.setControl('attributesValue', this.fb.array(attributesValueControls));
    if (this.disabled) {
      this.attributesValueFormGroup.disable({emitEvent: false});
    } else {
      this.attributesValueFormGroup.enable({emitEvent: false});
    }
    this.valueChange$ = this.attributesValueFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.updateUsedAttributesName();
  }

  attributesValueFormArray(): FormArray {
    return this.attributesValueFormGroup.get('attributesValue') as FormArray;
  }

  public removeKeyVal(index: number) {
    this.attributesValueFormArray().removeAt(index);
  }

  public addKeyVal() {
    this.attributesValueFormArray().push(this.createdFormGroup());
    this.attributesValueFormGroup.updateValueAndValidity({emitEvent: false});
    if (this.attributesValueFormGroup.invalid) {
      this.updateModel();
    }
  }

  private createdFormGroup(value?: AttributesNameValue): FormGroup {
    if (isUndefinedOrNull(value)) {
      value = {
        name: this.getFirstUnusedAttributesName(),
        value: null
      };
    }
    const form = this.fb.group({
      name: [value.name, Validators.required],
      value: [value.value, valueValidatorByAttributeName(value.name)]
    });
    form.get('name').valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(name => {
      form.get('value').setValidators(valueValidatorByAttributeName(name));
      form.get('value').updateValueAndValidity();
    });
    return form;
  }

  public validate() {
    return this.attributesValueFormGroup.valid ? null : {
      attributesValue: {
        valid: false
      }
    };
  }

  private updateModel() {
    const value: AttributesNameValue[] = this.attributesValueFormGroup.get('attributesValue').value;
    const attributesNameValueMap: AttributesNameValueMap = {};
    value.forEach(attribute => {
      attributesNameValueMap[attribute.name] = attribute.value;
    });
    this.updateUsedAttributesName();
    this.propagateChange(attributesNameValueMap);
  }

  public isDisabledAttributeName(type: AttributeName, index: number): boolean {
    const usedIndex = this.usedAttributesName.indexOf(type);
    return usedIndex > -1 && usedIndex !== index;
  }

  private getFirstUnusedAttributesName(): AttributeName {
    for (const attributeName of this.attributeNames) {
      if (this.usedAttributesName.indexOf(attributeName) === -1) {
        return attributeName;
      }
    }
    return null;
  }

  private updateUsedAttributesName() {
    this.usedAttributesName = [];
    const value: AttributesNameValue[] = this.attributesValueFormGroup.get('attributesValue').value;
    value.forEach((attributesValue, index) => {
      this.usedAttributesName[index] = attributesValue.name;
    });
  }

  get isAddEnabled(): boolean {
    return this.attributesValueFormArray().length !== this.attributeNames.length;
  }
}
