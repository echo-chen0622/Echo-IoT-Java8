import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormControl,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  Validator,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import {
  OpenStreetMapProvider,
  OpenStreetMapProviderSettings,
  openStreetMapProviderTranslationMap
} from '@home/components/widget/lib/maps/map-models';

@Component({
  selector: 'tb-openstreet-map-provider-settings',
  templateUrl: './openstreet-map-provider-settings.component.html',
  styleUrls: ['./../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => OpenStreetMapProviderSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => OpenStreetMapProviderSettingsComponent),
      multi: true
    }
  ]
})
export class OpenStreetMapProviderSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  private modelValue: OpenStreetMapProviderSettings;

  private propagateChange = null;

  public providerSettingsFormGroup: FormGroup;

  mapProviders = Object.values(OpenStreetMapProvider);

  openStreetMapProviderTranslations = openStreetMapProviderTranslationMap;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.providerSettingsFormGroup = this.fb.group({
      mapProvider: [null, [Validators.required]],
      useCustomProvider: [null, []],
      customProviderTileUrl: [null, [Validators.required]]
    });
    this.providerSettingsFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.providerSettingsFormGroup.get('useCustomProvider').valueChanges.subscribe(() => {
      this.updateValidators(true);
    });
    this.updateValidators(false);
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.providerSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.providerSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators(false);
    }
  }

  writeValue(value: OpenStreetMapProviderSettings): void {
    this.modelValue = value;
    this.providerSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators(false);
  }

  public validate(c: FormControl) {
    return this.providerSettingsFormGroup.valid ? null : {
      openStreetProviderSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: OpenStreetMapProviderSettings = this.providerSettingsFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }

  private updateValidators(emitEvent?: boolean): void {
    const useCustomProvider: boolean = this.providerSettingsFormGroup.get('useCustomProvider').value;
    if (useCustomProvider) {
      this.providerSettingsFormGroup.get('customProviderTileUrl').enable({emitEvent});
    } else {
      this.providerSettingsFormGroup.get('customProviderTileUrl').disable({emitEvent});
    }
    this.providerSettingsFormGroup.get('customProviderTileUrl').updateValueAndValidity({emitEvent: false});
  }
}
