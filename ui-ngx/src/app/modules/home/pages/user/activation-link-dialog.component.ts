import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {TranslateService} from '@ngx-translate/core';
import {ActionNotificationShow} from '@core/notification/notification.actions';
import {DialogComponent} from '@shared/components/dialog.component';
import {Router} from '@angular/router';

export interface ActivationLinkDialogData {
  activationLink: string;
}

@Component({
  selector: 'tb-activation-link-dialog',
  templateUrl: './activation-link-dialog.component.html'
})
export class ActivationLinkDialogComponent extends DialogComponent<ActivationLinkDialogComponent, void> implements OnInit {

  activationLink: string;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: ActivationLinkDialogData,
              public dialogRef: MatDialogRef<ActivationLinkDialogComponent, void>,
              private translate: TranslateService) {
    super(store, router, dialogRef);
    this.activationLink = this.data.activationLink;
  }

  ngOnInit(): void {
  }

  close(): void {
    this.dialogRef.close();
  }

  onActivationLinkCopied() {
     this.store.dispatch(new ActionNotificationShow(
       {
         message: this.translate.instant('user.activation-link-copied-message'),
         type: 'success',
         target: 'activationLinkDialogContent',
         duration: 1200,
         verticalPosition: 'bottom',
         horizontalPosition: 'left'
       }));
  }

}
