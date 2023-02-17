import { Component, forwardRef, Input, OnDestroy, OnInit } from '@angular/core';
import {
  ControlValueAccessor,
  FormBuilder,
  FormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validator,
  Validators
} from '@angular/forms';
import { coerceBooleanProperty } from '@angular/cdk/coercion';
import {
  DeviceProfileTransportConfiguration,
  DeviceTransportType,
  SnmpDeviceProfileTransportConfiguration
} from '@shared/models/device.models';
import { isDefinedAndNotNull } from '@core/utils';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

export interface OidMappingConfiguration {
  isAttribute: boolean;
  key: string;
  type: string;
  method: string;
  oid: string;
}

@Component({
  selector: 'tb-snmp-device-profile-transport-configuration',
  templateUrl: './snmp-device-profile-transport-configuration.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SnmpDeviceProfileTransportConfigurationComponent),
      multi: true
    },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => SnmpDeviceProfileTransportConfigurationComponent),
      multi: true
    }]
})
export class SnmpDeviceProfileTransportConfigurationComponent implements OnInit, OnDestroy, ControlValueAccessor, Validator {

  snmpDeviceProfileTransportConfigurationFormGroup: FormGroup;

  private destroy$ = new Subject();
  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }

  @Input()
  disabled: boolean;

  private propagateChange = (v: any) => {
  }

  constructor(private fb: FormBuilder) {
  }

  ngOnInit(): void {
    this.snmpDeviceProfileTransportConfigurationFormGroup = this.fb.group({
      timeoutMs: [500, [Validators.required, Validators.min(0), Validators.pattern('[0-9]*')]],
      retries: [0, [Validators.required, Validators.min(0), Validators.pattern('[0-9]*')]],
      communicationConfigs: [null, Validators.required],
    });
    this.snmpDeviceProfileTransportConfigurationFormGroup.valueChanges.pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.updateModel();
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean) {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.snmpDeviceProfileTransportConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.snmpDeviceProfileTransportConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: SnmpDeviceProfileTransportConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      this.snmpDeviceProfileTransportConfigurationFormGroup.patchValue(value, {emitEvent: !value.communicationConfigs});
    }
  }

  private updateModel() {
    let configuration: DeviceProfileTransportConfiguration = null;
    if (this.snmpDeviceProfileTransportConfigurationFormGroup.valid) {
      configuration = this.snmpDeviceProfileTransportConfigurationFormGroup.getRawValue();
      configuration.type = DeviceTransportType.SNMP;
    }
    this.propagateChange(configuration);
  }

  validate(): ValidationErrors | null {
    return this.snmpDeviceProfileTransportConfigurationFormGroup.valid ? null : {
      snmpDeviceProfileTransportConfiguration: false
    };
  }
}
