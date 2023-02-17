import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { DialogComponent } from '@app/shared/components/dialog.component';

export interface RateLimitsDetailsDialogData {
  rateLimits: string;
  title: string;
  readonly: boolean;
}

@Component({
  templateUrl: './rate-limits-details-dialog.component.html'
})
export class RateLimitsDetailsDialogComponent extends DialogComponent<RateLimitsDetailsDialogComponent> {

  editDetailsFormGroup: FormGroup;

  rateLimits: string = this.data.rateLimits;

  title: string = this.data.title;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: RateLimitsDetailsDialogData,
              public dialogRef: MatDialogRef<RateLimitsDetailsDialogComponent>,
              private fb: FormBuilder) {
    super(store, router, dialogRef);
    this.editDetailsFormGroup = this.fb.group({
      rateLimits: [this.rateLimits, []]
    });
    if (this.data.readonly) {
      this.editDetailsFormGroup.disable();
    }
  }

  save(): void {
    this.dialogRef.close(this.editDetailsFormGroup.get('rateLimits').value);
  }
}
