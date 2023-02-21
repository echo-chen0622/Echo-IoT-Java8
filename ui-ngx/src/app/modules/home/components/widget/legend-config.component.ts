import {Component, forwardRef, Input, OnDestroy, OnInit} from '@angular/core';
import {ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR} from '@angular/forms';
import {isDefined} from '@core/utils';
import {
  LegendConfig,
  LegendDirection,
  legendDirectionTranslationMap,
  LegendPosition,
  legendPositionTranslationMap
} from '@shared/models/widget.models';
import {Subscription} from 'rxjs';

// @dynamic
@Component({
  selector: 'tb-legend-config',
  templateUrl: './legend-config.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => LegendConfigComponent),
      multi: true
    }
  ]
})
export class LegendConfigComponent implements OnInit, OnDestroy, ControlValueAccessor {

  @Input() disabled: boolean;

  legendConfigForm: FormGroup;
  legendDirection = LegendDirection;
  legendDirections = Object.keys(LegendDirection);
  legendDirectionTranslations = legendDirectionTranslationMap;
  legendPosition = LegendPosition;
  legendPositions = Object.keys(LegendPosition);
  legendPositionTranslations = legendPositionTranslationMap;

  private legendSettingsFormChanges$: Subscription;
  private legendSettingsFormDirectionChanges$: Subscription;
  private propagateChange = (_: any) => {};

  constructor(private fb: FormBuilder) {
  }

  ngOnInit(): void {
    this.legendConfigForm = this.fb.group({
      direction: [null, []],
      position: [null, []],
      sortDataKeys: [null, []],
      showMin: [null, []],
      showMax: [null, []],
      showAvg: [null, []],
      showTotal: [null, []],
      showLatest: [null, []]
    });
    this.legendSettingsFormDirectionChanges$ = this.legendConfigForm.get('direction').valueChanges
      .subscribe((direction: LegendDirection) => {
        this.onDirectionChanged(direction);
      });
    this.legendSettingsFormChanges$ = this.legendConfigForm.valueChanges.subscribe(
      () => this.legendConfigUpdated()
    );
  }

  private onDirectionChanged(direction: LegendDirection) {
    if (direction === LegendDirection.row) {
      let position: LegendPosition = this.legendConfigForm.get('position').value;
      if (position !== LegendPosition.bottom && position !== LegendPosition.top) {
        position = LegendPosition.bottom;
      }
      this.legendConfigForm.patchValue({position}, {emitEvent: false}
      );
    }
  }

  ngOnDestroy(): void {
    if (this.legendSettingsFormDirectionChanges$) {
      this.legendSettingsFormDirectionChanges$.unsubscribe();
      this.legendSettingsFormDirectionChanges$ = null;
    }
    if (this.legendSettingsFormChanges$) {
      this.legendSettingsFormChanges$.unsubscribe();
      this.legendSettingsFormChanges$ = null;
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.legendConfigForm.disable({emitEvent: false});
    } else {
      this.legendConfigForm.enable({emitEvent: false});
    }
  }

  writeValue(legendConfig: LegendConfig): void {
    if (legendConfig) {
      this.legendConfigForm.patchValue({
        direction: legendConfig.direction,
        position: legendConfig.position,
        sortDataKeys: isDefined(legendConfig.sortDataKeys) ? legendConfig.sortDataKeys : false,
        showMin: isDefined(legendConfig.showMin) ? legendConfig.showMin : false,
        showMax: isDefined(legendConfig.showMax) ? legendConfig.showMax : false,
        showAvg: isDefined(legendConfig.showAvg) ? legendConfig.showAvg : false,
        showTotal: isDefined(legendConfig.showTotal) ? legendConfig.showTotal : false,
        showLatest: isDefined(legendConfig.showLatest) ? legendConfig.showLatest : false
      }, {emitEvent: false});
    }
    this.onDirectionChanged(legendConfig.direction);
  }

  private legendConfigUpdated() {
    this.propagateChange(this.legendConfigForm.value);
  }
}
