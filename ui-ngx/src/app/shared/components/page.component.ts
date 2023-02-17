import { Directive, OnDestroy } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Observable, Subscription } from 'rxjs';
import { selectIsLoading } from '@core/interceptors/load.selectors';
import { delay, share } from 'rxjs/operators';
import { AbstractControl } from '@angular/forms';

@Directive()
export abstract class PageComponent implements OnDestroy {

  isLoading$: Observable<boolean>;
  loadingSubscription: Subscription;
  disabledOnLoadFormControls: Array<AbstractControl> = [];

  protected constructor(protected store: Store<AppState>) {
    this.isLoading$ = this.store.pipe(delay(0), select(selectIsLoading), share());
  }

  protected registerDisableOnLoadFormControl(control: AbstractControl) {
    this.disabledOnLoadFormControls.push(control);
    if (!this.loadingSubscription) {
      this.loadingSubscription = this.isLoading$.subscribe((isLoading) => {
        for (const formControl of this.disabledOnLoadFormControls) {
          if (isLoading) {
            formControl.disable({emitEvent: false});
          } else {
            formControl.enable({emitEvent: false});
          }
        }
      });
    }
  }

  ngOnDestroy(): void {
    if (this.loadingSubscription) {
      this.loadingSubscription.unsubscribe();
    }
  }

}
