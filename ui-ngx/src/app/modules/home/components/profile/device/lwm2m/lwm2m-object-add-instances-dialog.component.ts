import {Component, Inject, OnInit} from '@angular/core';
import {DialogComponent} from '@shared/components/dialog.component';
import {FormBuilder, FormGroup} from '@angular/forms';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {Router} from '@angular/router';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';

export interface Lwm2mObjectAddInstancesData {
  instancesId: Set<number>;
  objectName?: string;
  objectId?: number;
}

@Component({
  selector: 'tb-lwm2m-object-add-instances',
  templateUrl: './lwm2m-object-add-instances-dialog.component.html'
})
export class Lwm2mObjectAddInstancesDialogComponent extends DialogComponent<Lwm2mObjectAddInstancesDialogComponent, object>
  implements OnInit {

  instancesFormGroup: FormGroup;
  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: Lwm2mObjectAddInstancesData,
              public dialogRef: MatDialogRef<Lwm2mObjectAddInstancesDialogComponent, object>,
              public fb: FormBuilder) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.instancesFormGroup = this.fb.group({
      instancesIds: [this.data.instancesId]
    });
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    this.dialogRef.close(this.instancesFormGroup.get('instancesIds').value);
  }
}
