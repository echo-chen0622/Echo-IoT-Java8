import { Component, Inject, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';

export interface EditAlarmDetailsDialogData {
  alarmDetails: string;
  readonly: boolean;
}

@Component({
  selector: 'tb-edit-alarm-details-dialog',
  templateUrl: './edit-alarm-details-dialog.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: EditAlarmDetailsDialogComponent}],
  styleUrls: []
})
export class EditAlarmDetailsDialogComponent extends DialogComponent<EditAlarmDetailsDialogComponent, string>
  implements OnInit, ErrorStateMatcher {

  alarmDetails = this.data.alarmDetails;

  editDetailsFormGroup: FormGroup;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: EditAlarmDetailsDialogData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public dialogRef: MatDialogRef<EditAlarmDetailsDialogComponent, string>,
              private fb: FormBuilder,
              private utils: UtilsService,
              public translate: TranslateService) {
    super(store, router, dialogRef);

    this.editDetailsFormGroup = this.fb.group({
      alarmDetails: [this.alarmDetails]
    });
    if (this.data.readonly) {
      this.editDetailsFormGroup.disable();
    }
  }

  ngOnInit(): void {
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  save(): void {
    this.submitted = true;
    this.alarmDetails = this.editDetailsFormGroup.get('alarmDetails').value;
    this.dialogRef.close(this.alarmDetails);
  }
}
