import {Component, Inject, OnInit, ViewChild} from '@angular/core';
import {MAT_DIALOG_DATA, MatDialog, MatDialogRef} from '@angular/material/dialog';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {FormGroup} from '@angular/forms';
import {UserComponent} from '@modules/home/pages/user/user.component';
import {Authority} from '@shared/models/authority.enum';
import {ActivationMethod, activationMethodTranslations, User} from '@shared/models/user.model';
import {CustomerId} from '@shared/models/id/customer-id';
import {UserService} from '@core/http/user.service';
import {Observable} from 'rxjs';
import {
  ActivationLinkDialogComponent,
  ActivationLinkDialogData
} from '@modules/home/pages/user/activation-link-dialog.component';
import {TenantId} from '@app/shared/models/id/tenant-id';
import {DialogComponent} from '@shared/components/dialog.component';
import {Router} from '@angular/router';

export interface AddUserDialogData {
  tenantId: string;
  customerId: string;
  authority: Authority;
}

@Component({
  selector: 'tb-add-user-dialog',
  templateUrl: './add-user-dialog.component.html',
  styleUrls: ['./add-user-dialog.component.scss']
})
export class AddUserDialogComponent extends DialogComponent<AddUserDialogComponent, User> implements OnInit {

  detailsForm: FormGroup;
  user: User;

  activationMethods = Object.keys(ActivationMethod);
  activationMethodEnum = ActivationMethod;

  activationMethodTranslations = activationMethodTranslations;

  activationMethod = ActivationMethod.DISPLAY_ACTIVATION_LINK;

  @ViewChild(UserComponent, {static: true}) userComponent: UserComponent;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              @Inject(MAT_DIALOG_DATA) public data: AddUserDialogData,
              public dialogRef: MatDialogRef<AddUserDialogComponent, User>,
              private userService: UserService,
              private dialog: MatDialog) {
    super(store, router, dialogRef);
  }

  ngOnInit(): void {
    this.user = {} as User;
    this.userComponent.isEdit = true;
    this.userComponent.entity = this.user;
    this.detailsForm = this.userComponent.entityForm;
  }

  cancel(): void {
    this.dialogRef.close(null);
  }

  add(): void {
    if (this.detailsForm.valid) {
      this.user = {...this.user, ...this.userComponent.entityForm.value};
      this.user.authority = this.data.authority;
      this.user.tenantId = new TenantId(this.data.tenantId);
      this.user.customerId = new CustomerId(this.data.customerId);
      const sendActivationEmail = this.activationMethod === ActivationMethod.SEND_ACTIVATION_MAIL;
      this.userService.saveUser(this.user, sendActivationEmail).subscribe(
        (user) => {
          if (this.activationMethod === ActivationMethod.DISPLAY_ACTIVATION_LINK) {
            this.userService.getActivationLink(user.id.id).subscribe(
              (activationLink) => {
                this.displayActivationLink(activationLink).subscribe(
                  () => {
                    this.dialogRef.close(user);
                  }
                );
              }
            );
          } else {
            this.dialogRef.close(user);
          }
        }
      );
    }
  }

  displayActivationLink(activationLink: string): Observable<void> {
    return this.dialog.open<ActivationLinkDialogComponent, ActivationLinkDialogData,
      void>(ActivationLinkDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        activationLink
      }
    }).afterClosed();
  }
}
