import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {
  Component,
  ComponentFactory,
  ComponentRef,
  HostBinding,
  Inject,
  Injector,
  OnDestroy,
  ViewContainerRef
} from '@angular/core';
import {DialogComponent} from '@shared/components/dialog.component';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {Router} from '@angular/router';
import {
  CUSTOM_DIALOG_DATA,
  CustomDialogComponent,
  CustomDialogData
} from '@home/components/widget/dialog/custom-dialog.component';

export interface CustomDialogContainerData {
  controller: (instance: CustomDialogComponent) => void;
  data?: any;
  customComponentFactory: ComponentFactory<CustomDialogComponent>;
}

@Component({
  selector: 'tb-custom-dialog-container-component',
  template: ''
})
export class CustomDialogContainerComponent extends DialogComponent<CustomDialogContainerComponent> implements OnDestroy {

  @HostBinding('style.height') height = '0px';

  private readonly customComponentRef: ComponentRef<CustomDialogComponent>;

  constructor(protected store: Store<AppState>,
              protected router: Router,
              public viewContainerRef: ViewContainerRef,
              public dialogRef: MatDialogRef<CustomDialogContainerComponent>,
              @Inject(MAT_DIALOG_DATA) public data: CustomDialogContainerData) {
    super(store, router, dialogRef);
    let customDialogData: CustomDialogData = {
      controller: this.data.controller
    };
    if (this.data.data) {
      customDialogData = {...customDialogData, ...this.data.data};
    }
    const injector: Injector = Injector.create({
      providers: [{
        provide: CUSTOM_DIALOG_DATA,
        useValue: customDialogData
      },
        {
          provide: MatDialogRef,
          useValue: dialogRef
        }]
    });
    this.customComponentRef = this.viewContainerRef.createComponent(this.data.customComponentFactory, 0, injector);
  }

  ngOnDestroy(): void {
    super.ngOnDestroy();
    if (this.customComponentRef) {
      this.customComponentRef.destroy();
    }
  }

}
