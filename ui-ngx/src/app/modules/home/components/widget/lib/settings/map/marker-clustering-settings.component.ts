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
import { MarkerClusteringSettings } from '@home/components/widget/lib/maps/map-models';
import { WidgetService } from '@core/http/widget.service';

@Component({
  selector: 'tb-marker-clustering-settings',
  templateUrl: './marker-clustering-settings.component.html',
  styleUrls: ['./../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MarkerClusteringSettingsComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => MarkerClusteringSettingsComponent),
      multi: true
    }
  ]
})
export class MarkerClusteringSettingsComponent extends PageComponent implements OnInit, ControlValueAccessor, Validator {

  @Input()
  disabled: boolean;

  private modelValue: MarkerClusteringSettings;

  functionScopeVariables = this.widgetService.getWidgetScopeVariables();

  private propagateChange = null;

  public markerClusteringSettingsFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private widgetService: WidgetService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.markerClusteringSettingsFormGroup = this.fb.group({
      useClusterMarkers: [null, []],
      zoomOnClick: [null, []],
      maxZoom: [null, [Validators.min(0), Validators.max(18)]],
      maxClusterRadius: [null, [Validators.min(0)]],
      animate: [null, []],
      spiderfyOnMaxZoom: [null, []],
      showCoverageOnHover: [null, []],
      chunkedLoading: [null, []],
      removeOutsideVisibleBounds: [null, []],
      useIconCreateFunction: [null, []],
      clusterMarkerFunction: [null, []]
    });
    this.markerClusteringSettingsFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.markerClusteringSettingsFormGroup.get('useClusterMarkers').valueChanges.subscribe(() => {
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
      this.markerClusteringSettingsFormGroup.disable({emitEvent: false});
    } else {
      this.markerClusteringSettingsFormGroup.enable({emitEvent: false});
      this.updateValidators(false);
    }
  }

  writeValue(value: MarkerClusteringSettings): void {
    this.modelValue = value;
    this.markerClusteringSettingsFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators(false);
  }

  public validate(c: FormControl) {
    return this.markerClusteringSettingsFormGroup.valid ? null : {
      markerClusteringSettings: {
        valid: false,
      },
    };
  }

  private updateModel() {
    const value: MarkerClusteringSettings = this.markerClusteringSettingsFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }

  private updateValidators(emitEvent?: boolean): void {
    const useClusterMarkers: boolean = this.markerClusteringSettingsFormGroup.get('useClusterMarkers').value;

    this.markerClusteringSettingsFormGroup.disable({emitEvent: false});
    this.markerClusteringSettingsFormGroup.get('useClusterMarkers').enable({emitEvent: false});

    if (useClusterMarkers) {
      this.markerClusteringSettingsFormGroup.enable({emitEvent: false});
    }
    this.markerClusteringSettingsFormGroup.updateValueAndValidity({emitEvent: false});
  }
}
