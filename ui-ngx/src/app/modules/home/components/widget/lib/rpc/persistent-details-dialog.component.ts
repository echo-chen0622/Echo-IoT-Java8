import { Component, ElementRef, Inject, OnInit, ViewChild } from '@angular/core';
import { DialogComponent } from '@shared/components/dialog.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { FormBuilder, FormGroup } from '@angular/forms';
import { DeviceService } from '@core/http/device.service';
import { PersistentRpc, RpcStatus, rpcStatusColors, rpcStatusTranslation } from '@shared/models/rpc.models';
import { NULL_UUID } from '@shared/models/id/has-uuid';
import { DialogService } from '@core/services/dialog.service';

export interface PersistentDetailsDialogData {
  persistentRequest: PersistentRpc;
  allowDelete: boolean;
}

@Component({
  selector: 'tb-persistent-details-dialog',
  templateUrl: './persistent-details-dialog.component.html',
  styleUrls: ['./persistent-details-dialog.component.scss']
})

export class PersistentDetailsDialogComponent extends DialogComponent<PersistentDetailsDialogComponent, boolean> implements OnInit {

  @ViewChild('responseDataEditor', {static: true})
  responseDataEditorElmRef: ElementRef;

  public persistentFormGroup: FormGroup;
  public rpcStatusColorsMap = rpcStatusColors;
  public rpcStatus = RpcStatus;
  public allowDelete: boolean;

  private persistentUpdated = false;
  private responseData: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              private datePipe: DatePipe,
              private translate: TranslateService,
              @Inject(MAT_DIALOG_DATA) public data: PersistentDetailsDialogData,
              public dialogRef: MatDialogRef<PersistentDetailsDialogComponent, boolean>,
              private dialogService: DialogService,
              private deviceService: DeviceService,
              private fb: FormBuilder) {
    super(store, router, dialogRef);

    this.allowDelete = data.allowDelete;

    this.persistentFormGroup = this.fb.group(
      {
        rpcId: [''],
        createdTime: [''],
        expirationTime: [''],
        messageType: [''],
        status: [''],
        method: [''],
        params: [''],
        retries: [''],
        response: [''],
        additionalInfo: ['']
      }
    );
    this.loadPersistentFields(data.persistentRequest);
    this.responseData = JSON.stringify(data.persistentRequest.response, null, 2);
  }

  loadPersistentFields(request: PersistentRpc) {
    this.persistentFormGroup.patchValue({
      rpcId: this.translate.instant('widgets.persistent-table.details-title') + request.id.id,
      createdTime: this.datePipe.transform(request.createdTime, 'yyyy-MM-dd HH:mm:ss'),
      expirationTime: this.datePipe.transform(request.expirationTime, 'yyyy-MM-dd HH:mm:ss'),
      messageType: this.translate.instant('widgets.persistent-table.message-types.' + request.request.oneway),
      status: this.translate.instant(rpcStatusTranslation.get(request.status)),
      method: request.request.body.method,
      retries: request.request.retries || null,
      response: request.response || null,
      params: JSON.parse(request.request.body.params) || null,
      additionalInfo: request.additionalInfo || null
    }, {emitEvent: false});
  }

  ngOnInit(): void {
  }

  close(): void {
    this.dialogRef.close(this.persistentUpdated);
  }

  deleteRpcRequest() {
    const persistentRpc = this.data.persistentRequest;
    if (persistentRpc && persistentRpc.id && persistentRpc.id.id !== NULL_UUID) {
      this.dialogService.confirm(
        this.translate.instant('widgets.persistent-table.delete-request-title'),
        this.translate.instant('widgets.persistent-table.delete-request-text'),
        this.translate.instant('action.no'),
        this.translate.instant('action.yes')
      ).subscribe((res) => {
        if (res) {
          this.deviceService.deletePersistedRpc(persistentRpc.id.id).subscribe(() => {
            this.persistentUpdated = true;
            this.close();
          });
        }
      });
    }
  }
}
