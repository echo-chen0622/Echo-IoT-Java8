import {MatDialogRef} from '@angular/material/dialog';
import {Directive, InjectionToken} from '@angular/core';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {Router} from '@angular/router';
import {PageComponent} from '@shared/components/page.component';
import {CustomDialogContainerComponent} from './custom-dialog-container.component';
import {FormBuilder, Validators} from '@angular/forms';
import {TbInject} from '@shared/decorators/tb-inject';

export const CUSTOM_DIALOG_DATA = new InjectionToken<any>('ConfigDialogData');

export interface CustomDialogData {
  controller: (instance: CustomDialogComponent) => void;
  [key: string]: any;
}

@Directive()
// tslint:disable-next-line:directive-class-suffix
export class CustomDialogComponent extends PageComponent {

  [key: string]: any;

  constructor(@TbInject(Store) protected store: Store<AppState>,
              @TbInject(Router) protected router: Router,
              @TbInject(MatDialogRef) public dialogRef: MatDialogRef<CustomDialogContainerComponent>,
              @TbInject(FormBuilder) public fb: FormBuilder,
              @TbInject(CUSTOM_DIALOG_DATA) public data: CustomDialogData) {
    super(store);
    // @ts-ignore
    this.validators = Validators;
    this.data.controller(this);
  }
}
