import { Component } from '@angular/core';
import { WidgetSettings, WidgetSettingsComponent } from '@shared/models/widget.models';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-qrcode-widget-settings',
  templateUrl: './qrcode-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class QrCodeWidgetSettingsComponent extends WidgetSettingsComponent {

  qrCodeWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.qrCodeWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      qrCodeTextPattern: '${entityName}',
      useQrCodeTextFunction: false,
      qrCodeTextFunction: 'return data[0][\'entityName\'];'
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.qrCodeWidgetSettingsForm = this.fb.group({
      qrCodeTextPattern: [settings.qrCodeTextPattern, [Validators.required]],
      useQrCodeTextFunction: [settings.useQrCodeTextFunction, [Validators.required]],
      qrCodeTextFunction: [settings.qrCodeTextFunction, [Validators.required]]
    });
  }

  protected validatorTriggers(): string[] {
    return ['useQrCodeTextFunction'];
  }

  protected updateValidators(emitEvent: boolean) {
    const useQrCodeTextFunction: boolean = this.qrCodeWidgetSettingsForm.get('useQrCodeTextFunction').value;
    if (useQrCodeTextFunction) {
      this.qrCodeWidgetSettingsForm.get('qrCodeTextPattern').disable();
      this.qrCodeWidgetSettingsForm.get('qrCodeTextFunction').enable();
    } else {
      this.qrCodeWidgetSettingsForm.get('qrCodeTextPattern').enable();
      this.qrCodeWidgetSettingsForm.get('qrCodeTextFunction').disable();
    }
    this.qrCodeWidgetSettingsForm.get('qrCodeTextPattern').updateValueAndValidity({emitEvent});
    this.qrCodeWidgetSettingsForm.get('qrCodeTextFunction').updateValueAndValidity({emitEvent});
  }

}
