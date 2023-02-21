import {Component, forwardRef, Input, OnInit} from '@angular/core';
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
import {PageComponent} from '@shared/components/page.component';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {TranslateService} from '@ngx-translate/core';
import {
  HereMapProvider,
  HereMapProviderSettings,
  hereMapProviderTranslationMap
} from '@home/components/widget/lib/maps/map-models';

@Component({
  selector: 'tb-here-map-provider-settings',
  templateUrl: './here-map-provider-settings.component.html',
  styleUrls: ['./../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => HereMapProviderSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => HereMapProviderSettingsComponent),
      multi: true
    }
  ]
})
export class HereMapProviderSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  private modelValue: HereMapProviderSettings;

  private propagateChange = null;

  public providerSettingsFormGroup: FormGroup;

  hereMapProviders = Object.values(HereMapProvider);

  hereMapProviderTranslations = hereMapProviderTranslationMap;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.providerSettingsFormGroup = this.fb.group({
      mapProviderHere: [null, [Validators.required]],
      credentials: this.fb.group({
        app_id: [null, [Validators.required]],
        app_code: [null, [Validators.required]]
      })
    });
    this.providerSettingsFormGroup.valueChanges.subscribe(() => {
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
      this.providerSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.providerSettingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: HereMapProviderSettings): void {
    this.modelValue = value;
    this.providerSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
  }

  public validate(c: FormControl) {
    return this.providerSettingsFormGroup.valid ? null : {
      hereMapProviderSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: HereMapProviderSettings = this.providerSettingsFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }
}
