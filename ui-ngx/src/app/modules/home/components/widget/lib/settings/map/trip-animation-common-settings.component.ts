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
import { TripAnimationCommonSettings } from '@home/components/widget/lib/maps/map-models';
import { Widget } from '@shared/models/widget.models';
import { WidgetService } from '@core/http/widget.service';

@Component({
  selector: 'tb-trip-animation-common-settings',
  templateUrl: './trip-animation-common-settings.component.html',
  styleUrls: ['./../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => TripAnimationCommonSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => TripAnimationCommonSettingsComponent),
      multi: true
    }
  ]
})
export class TripAnimationCommonSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  @Input()
  widget: Widget;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  private modelValue: TripAnimationCommonSettings;

  private propagateChange = null;

  public tripAnimationCommonSettingsFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private widgetService: WidgetService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.tripAnimationCommonSettingsFormGroup = this.fb.group({
      normalizationStep: [null, [Validators.min(1)]],
      latKeyName: [null, [Validators.required]],
      lngKeyName: [null, [Validators.required]],
      showTooltip: [null, []],
      tooltipColor: [null, []],
      tooltipFontColor: [null, []],
      tooltipOpacity: [null, [Validators.min(0), Validators.max(1)]],
      autocloseTooltip: [null, []],
      useTooltipFunction: [null, []],
      tooltipPattern: [null, []],
      tooltipFunction: [null, []],
    });
    this.tripAnimationCommonSettingsFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.tripAnimationCommonSettingsFormGroup.get('showTooltip').valueChanges.subscribe(() => {
      this.updateValidators(true);
    });
    this.tripAnimationCommonSettingsFormGroup.get('useTooltipFunction').valueChanges.subscribe(() => {
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
      this.tripAnimationCommonSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.tripAnimationCommonSettingsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: TripAnimationCommonSettings): void {
    this.modelValue = value;
    this.tripAnimationCommonSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators(false);
  }

  public validate(c: FormControl) {
    return this.tripAnimationCommonSettingsFormGroup.valid ? null : {
      tripAnimationCommonSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: TripAnimationCommonSettings = this.tripAnimationCommonSettingsFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }

  private updateValidators(emitEvent?: boolean): void {
    const showTooltip: boolean = this.tripAnimationCommonSettingsFormGroup.get('showTooltip').value;
    const useTooltipFunction: boolean = this.tripAnimationCommonSettingsFormGroup.get('useTooltipFunction').value;
    if (showTooltip) {
      this.tripAnimationCommonSettingsFormGroup.get('tooltipColor').enable({emitEvent});
      this.tripAnimationCommonSettingsFormGroup.get('tooltipFontColor').enable({emitEvent});
      this.tripAnimationCommonSettingsFormGroup.get('tooltipOpacity').enable({emitEvent});
      this.tripAnimationCommonSettingsFormGroup.get('autocloseTooltip').enable({emitEvent});
      this.tripAnimationCommonSettingsFormGroup.get('useTooltipFunction').enable({emitEvent: false});
      if (useTooltipFunction) {
        this.tripAnimationCommonSettingsFormGroup.get('tooltipFunction').enable({emitEvent});
        this.tripAnimationCommonSettingsFormGroup.get('tooltipPattern').disable({emitEvent});
      } else {
        this.tripAnimationCommonSettingsFormGroup.get('tooltipFunction').disable({emitEvent});
        this.tripAnimationCommonSettingsFormGroup.get('tooltipPattern').enable({emitEvent});
      }
    } else {
      this.tripAnimationCommonSettingsFormGroup.get('tooltipColor').disable({emitEvent});
      this.tripAnimationCommonSettingsFormGroup.get('tooltipFontColor').disable({emitEvent});
      this.tripAnimationCommonSettingsFormGroup.get('tooltipOpacity').disable({emitEvent});
      this.tripAnimationCommonSettingsFormGroup.get('autocloseTooltip').disable({emitEvent});
      this.tripAnimationCommonSettingsFormGroup.get('useTooltipFunction').disable({emitEvent: false});
      this.tripAnimationCommonSettingsFormGroup.get('tooltipFunction').disable({emitEvent});
      this.tripAnimationCommonSettingsFormGroup.get('tooltipPattern').disable({emitEvent});
    }
    this.tripAnimationCommonSettingsFormGroup.get('tooltipColor').updateValueAndValidity({emitEvent: false});
    this.tripAnimationCommonSettingsFormGroup.get('tooltipFontColor').updateValueAndValidity({emitEvent: false});
    this.tripAnimationCommonSettingsFormGroup.get('tooltipOpacity').updateValueAndValidity({emitEvent: false});
    this.tripAnimationCommonSettingsFormGroup.get('autocloseTooltip').updateValueAndValidity({emitEvent: false});
    this.tripAnimationCommonSettingsFormGroup.get('useTooltipFunction').updateValueAndValidity({emitEvent: false});
    this.tripAnimationCommonSettingsFormGroup.get('tooltipFunction').updateValueAndValidity({emitEvent: false});
    this.tripAnimationCommonSettingsFormGroup.get('tooltipPattern').updateValueAndValidity({emitEvent: false});
  }
}
