import { Injectable } from '@angular/core';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { map } from 'rxjs/operators';

import { NotificationActions, NotificationActionTypes } from '@app/core/notification/notification.actions';
import { NotificationService } from '@app/core/services/notification.service';

@Injectable()
export class NotificationEffects {
  constructor(
    private actions$: Actions<NotificationActions>,
    private notificationService: NotificationService
  ) {
  }

  @Effect({dispatch: false})
  dispatchNotification = this.actions$.pipe(
    ofType(
      NotificationActionTypes.SHOW_NOTIFICATION,
    ),
    map(({ notification }) => {
      this.notificationService.dispatchNotification(notification);
    })
  );

  @Effect({dispatch: false})
  hideNotification = this.actions$.pipe(
    ofType(
      NotificationActionTypes.HIDE_NOTIFICATION,
    ),
    map(({ hideNotification }) => {
      this.notificationService.hideNotification(hideNotification);
    })
  );
}
