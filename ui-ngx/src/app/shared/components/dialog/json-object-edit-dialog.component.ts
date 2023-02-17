import {Component, Inject, OnInit} from '@angular/core';
import {DialogComponent} from '@shared/components/dialog.component';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {Router} from '@angular/router';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {FormBuilder, FormGroup} from '@angular/forms';
import {TranslateService} from '@ngx-translate/core';

export interface JsonObjectEditDialogData {
  jsonValue: object;
  title?: string;
}

@Component({
  selector: 'tb-object-edit-dialog',
  templateUrl: './json-object-edit-dialog.component.html',
  styleUrls: []
})
export class JsonObjectEditDialogComponent extends DialogComponent<JsonObjectEditDialogComponent, object> implements OnInit {

  jsonFormGroup: FormGroup;
  title: string;

  submitted = false;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: JsonObjectEditDialogData,
              public dialogRef: MatDialogRef<JsonObjectEditDialogComponent, object>,
              public fb: FormBuilder,
              private translate: TranslateService) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.title = this.data.title ? this.data.title : this.translate.instant('details.edit-json');
    this.jsonFormGroup = this.fb.group({
      json: [this.data.jsonValue, []]
    });
  }

  cancel(): void {
    this.dialogRef.close(undefined);
  }

  add(): void {
    this.dialogRef.close(this.jsonFormGroup.get('json').value);
  }
}
