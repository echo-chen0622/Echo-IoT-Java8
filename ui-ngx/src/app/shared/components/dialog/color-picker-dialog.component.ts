import {Component, Inject, OnInit, SkipSelf} from '@angular/core';
import {ErrorStateMatcher} from '@angular/material/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators} from '@angular/forms';
import {Router} from '@angular/router';
import {DialogComponent} from '@shared/components/dialog.component';

export interface ColorPickerDialogData {
  color: string;
}

@Component({
  selector: 'tb-color-picker-dialog',
  templateUrl: './color-picker-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: ColorPickerDialogComponent}],
  styleUrls: []
})
export class ColorPickerDialogComponent extends DialogComponent<ColorPickerDialogComponent, string>
  implements OnInit, ErrorStateMatcher {

  colorPickerFormGroup: FormGroup;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ColorPickerDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<ColorPickerDialogComponent, string>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.colorPickerFormGroup = this.fb.group({
      color: [this.data.color, [Validators.required]]
    });
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  onColorChange(color: string) {
    this.colorPickerFormGroup.get('color').setValue(color);
    this.colorPickerFormGroup.markAsDirty();
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  select(): void {
    this.submitted = true;
    const color: string = this.colorPickerFormGroup.get('color').value;
    this.dialogRef.close(color);
  }
}
