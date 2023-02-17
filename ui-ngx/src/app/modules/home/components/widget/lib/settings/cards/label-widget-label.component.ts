import {Component, EventEmitter, forwardRef, Input, OnInit, Output} from '@angular/core';
import {ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators} from '@angular/forms';
import {PageComponent} from '@shared/components/page.component';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {TranslateService} from '@ngx-translate/core';
import {WidgetFont} from '@home/components/widget/lib/settings/common/widget-font.component';

export interface LabelWidgetLabel {
  pattern: string;
  x: number;
  y: number;
  backgroundColor: string;
  font: WidgetFont;
}

@Component({
  selector: 'tb-label-widget-label',
  templateUrl: './label-widget-label.component.html',
  styleUrls: ['./label-widget-label.component.scss', './../widget-settings.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => LabelWidgetLabelComponent),
      multi: true
    }
  ]
})
export class LabelWidgetLabelComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  expanded = false;

  @Output()
  removeLabel = new EventEmitter();

  private modelValue: LabelWidgetLabel;

  private propagateChange = null;

  public labelWidgetLabelFormGroup: FormGroup;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.labelWidgetLabelFormGroup = this.fb.group({
      pattern: [null, [Validators.required]],
      x: [null, [Validators.min(0), Validators.max(100)]],
      y: [null, [Validators.min(0), Validators.max(100)]],
      backgroundColor: [null, []],
      font: [null, []]
    });
    this.labelWidgetLabelFormGroup.valueChanges.subscribe(() => {
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
      this.labelWidgetLabelFormGroup.disable({emitEvent: false});
    } else {
      this.labelWidgetLabelFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: LabelWidgetLabel): void {
    this.modelValue = value;
    this.labelWidgetLabelFormGroup.patchValue(
      value, {emitEvent: false}
    );
  }

  private updateModel() {
    const value: LabelWidgetLabel = this.labelWidgetLabelFormGroup.value;
    this.modelValue = value;
    if (this.labelWidgetLabelFormGroup.valid) {
      this.propagateChange(this.modelValue);
    } else {
      this.propagateChange(null);
    }
  }
}
