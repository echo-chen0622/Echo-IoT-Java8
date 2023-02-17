import { Component, Inject, InjectionToken, OnInit, SkipSelf } from '@angular/core';
import { ErrorStateMatcher } from '@angular/material/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormControl, FormGroup, FormGroupDirective, NgForm, Validators } from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { OverlayRef } from '@angular/cdk/overlay';

export const EDIT_ATTRIBUTE_VALUE_PANEL_DATA = new InjectionToken<any>('EditAttributeValuePanelData');

export interface EditAttributeValuePanelData {
  attributeValue: any;
}

@Component({
  selector: 'tb-edit-attribute-value-panel',
  templateUrl: './edit-attribute-value-panel.component.html',
  providers: [{provide: ErrorStateMatcher, useExisting: EditAttributeValuePanelComponent}],
  styleUrls: ['./edit-attribute-value-panel.component.scss']
})
export class EditAttributeValuePanelComponent extends PageComponent implements OnInit, ErrorStateMatcher {

  attributeFormGroup: FormGroup;

  result: any = null;

  submitted = false;

  constructor(protected store: Store<AppState>,
              @Inject(EDIT_ATTRIBUTE_VALUE_PANEL_DATA) public data: EditAttributeValuePanelData,
              @SkipSelf() private errorStateMatcher: ErrorStateMatcher,
              public overlayRef: OverlayRef,
              public fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.attributeFormGroup = this.fb.group({
      value: [this.data.attributeValue, [Validators.required]]
    });
  }

  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const originalErrorState = this.errorStateMatcher.isErrorState(control, form);
    const customErrorState = !!(control && control.invalid && this.submitted);
    return originalErrorState || customErrorState;
  }

  cancel(): void {
    this.overlayRef.dispose();
  }

  update(): void {
    this.submitted = true;
    this.result = this.attributeFormGroup.get('value').value;
    this.overlayRef.dispose();
  }
}
