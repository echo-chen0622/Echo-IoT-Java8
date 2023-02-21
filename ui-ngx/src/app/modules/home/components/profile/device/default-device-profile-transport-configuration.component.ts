import {Component, forwardRef, Input, OnInit} from '@angular/core';
import {ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators} from '@angular/forms';
import {Store} from '@ngrx/store';
import {AppState} from '@app/core/core.state';
import {coerceBooleanProperty} from '@angular/cdk/coercion';
import {
  DefaultDeviceProfileTransportConfiguration,
  DeviceProfileTransportConfiguration,
  DeviceTransportType
} from '@shared/models/device.models';

@Component({
  selector: 'tb-default-device-profile-transport-configuration',
  templateUrl: './default-device-profile-transport-configuration.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => DefaultDeviceProfileTransportConfigurationComponent),
    multi: true
  }]
})
export class DefaultDeviceProfileTransportConfigurationComponent implements ControlValueAccessor, OnInit {

  defaultDeviceProfileTransportConfigurationFormGroup: FormGroup;

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

  private propagateChange = (v: any) => { };

  constructor(private store: Store<AppState>,
              private fb: FormBuilder) {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.defaultDeviceProfileTransportConfigurationFormGroup = this.fb.group({
      configuration: [null, Validators.required]
    });
    this.defaultDeviceProfileTransportConfigurationFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.defaultDeviceProfileTransportConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.defaultDeviceProfileTransportConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: DefaultDeviceProfileTransportConfiguration | null): void {
    this.defaultDeviceProfileTransportConfigurationFormGroup.patchValue({configuration: value}, {emitEvent: false});
  }

  private updateModel() {
    let configuration: DeviceProfileTransportConfiguration = null;
    if (this.defaultDeviceProfileTransportConfigurationFormGroup.valid) {
      configuration = this.defaultDeviceProfileTransportConfigurationFormGroup.getRawValue().configuration;
      configuration.type = DeviceTransportType.DEFAULT;
    }
    this.propagateChange(configuration);
  }
}
