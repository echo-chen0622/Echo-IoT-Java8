import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, CanDeactivate, RouterStateSnapshot} from '@angular/router';
import {FormGroup} from '@angular/forms';
import {select, Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {AuthState} from '@core/auth/auth.models';
import {selectAuth} from '@core/auth/auth.selectors';
import {map, take} from 'rxjs/operators';
import {DialogService} from '@core/services/dialog.service';
import {TranslateService} from '@ngx-translate/core';
import {isDefined} from '../utils';

export interface HasConfirmForm {
  confirmForm(): FormGroup;
}

export interface HasDirtyFlag {
  isDirty: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ConfirmOnExitGuard implements CanDeactivate<HasConfirmForm & HasDirtyFlag> {

  constructor(private store: Store<AppState>,
              private dialogService: DialogService,
              private translate: TranslateService) { }

  canDeactivate(component: HasConfirmForm & HasDirtyFlag,
                route: ActivatedRouteSnapshot,
                state: RouterStateSnapshot) {


    let auth: AuthState = null;
    this.store.pipe(select(selectAuth), take(1)).subscribe(
      (authState: AuthState) => {
        auth = authState;
      }
    );

    if (auth && auth.isAuthenticated) {
      let isDirty = false;
      if (component.confirmForm) {
        const confirmForm = component.confirmForm();
        if (confirmForm) {
          isDirty = confirmForm.dirty;
        }
      } else if (isDefined(component.isDirty)) {
        isDirty = component.isDirty;
      }
      if (isDirty) {
        return this.dialogService.confirm(
          this.translate.instant('confirm-on-exit.title'),
          this.translate.instant('confirm-on-exit.html-message')
        ).pipe(
          map((dialogResult) => {
            if (dialogResult) {
              if (component.confirmForm && component.confirmForm()) {
                component.confirmForm().markAsPristine();
              } else {
                component.isDirty = false;
              }
            }
            return dialogResult;
          })
        );
      }
    }
    return true;
  }
}
