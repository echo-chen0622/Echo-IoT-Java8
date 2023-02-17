import {Component} from '@angular/core';
import {WidgetSettings, WidgetSettingsComponent} from '@shared/models/widget.models';
import {AbstractControl, FormArray, FormBuilder, FormGroup, Validators} from '@angular/forms';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {GpioItem, gpioItemValidator} from '@home/components/widget/lib/settings/gpio/gpio-item.component';

@Component({
  selector: 'tb-gpio-panel-widget-settings',
  templateUrl: './gpio-panel-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class GpioPanelWidgetSettingsComponent extends WidgetSettingsComponent {

  gpioPanelWidgetSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  protected settingsForm(): FormGroup {
    return this.gpioPanelWidgetSettingsForm;
  }

  protected defaultSettings(): WidgetSettings {
    return {
      ledPanelBackgroundColor: '#008a00',
      gpioList: []
    };
  }

  protected onSettingsSet(settings: WidgetSettings) {
    this.gpioPanelWidgetSettingsForm = this.fb.group({

      // Panel settings

      ledPanelBackgroundColor: [settings.ledPanelBackgroundColor, [Validators.required]],

      // --> GPIO leds

      gpioList: this.prepareGpioListFormArray(settings.gpioList),

    });
  }

  protected doUpdateSettings(settingsForm: FormGroup, settings: WidgetSettings) {
    settingsForm.setControl('gpioList', this.prepareGpioListFormArray(settings.gpioList), {emitEvent: false});
  }

  private prepareGpioListFormArray(gpioList: GpioItem[] | undefined): FormArray {
    const gpioListControls: Array<AbstractControl> = [];
    if (gpioList) {
      gpioList.forEach((gpioItem) => {
        gpioListControls.push(this.fb.control(gpioItem, [gpioItemValidator(true)]));
      });
    }
    return this.fb.array(gpioListControls, [(control: AbstractControl) => {
      const gpioItems = control.value;
      if (!gpioItems || !gpioItems.length) {
        return {
          gpioItems: true
        };
      }
      return null;
    }]);
  }

  gpioListFormArray(): FormArray {
    return this.gpioPanelWidgetSettingsForm.get('gpioList') as FormArray;
  }

  public trackByGpioItem(index: number, gpioItemControl: AbstractControl): any {
    return gpioItemControl;
  }

  public removeGpioItem(index: number) {
    (this.gpioPanelWidgetSettingsForm.get('gpioList') as FormArray).removeAt(index);
  }

  public addGpioItem() {
    const gpioItem: GpioItem = {
      pin: null,
      label: null,
      row: null,
      col: null,
      color: null
    };
    const gpioListArray = this.gpioPanelWidgetSettingsForm.get('gpioList') as FormArray;
    const gpioItemControl = this.fb.control(gpioItem, [gpioItemValidator(true)]);
    (gpioItemControl as any).new = true;
    gpioListArray.push(gpioItemControl);
    this.gpioPanelWidgetSettingsForm.updateValueAndValidity();
    if (!this.gpioPanelWidgetSettingsForm.valid) {
      this.onSettingsChanged(this.gpioPanelWidgetSettingsForm.value);
    }
  }
}
