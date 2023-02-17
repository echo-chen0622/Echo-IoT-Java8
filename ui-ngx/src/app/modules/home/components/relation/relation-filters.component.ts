import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  FormArray,
  FormBuilder,
  FormGroup,
  NG_VALUE_ACCESSOR
} from '@angular/forms';
import { AliasEntityType, EntityType } from '@shared/models/entity-type.models';
import { RelationEntityTypeFilter } from '@shared/models/relation.models';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { Subscription } from 'rxjs';

@Component({
  selector: 'tb-relation-filters',
  templateUrl: './relation-filters.component.html',
  styleUrls: ['./relation-filters.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RelationFiltersComponent),
      multi: true
    }
  ]
})
export class RelationFiltersComponent extends PageComponent implements ControlValueAccessor, OnInit {

  @Input() disabled: boolean;

  @Input() allowedEntityTypes: Array<EntityType | AliasEntityType>;

  relationFiltersFormGroup: FormGroup;

  private propagateChange = null;

  private valueChangeSubscription: Subscription = null;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.relationFiltersFormGroup = this.fb.group({});
    this.relationFiltersFormGroup.addControl('relationFilters',
      this.fb.array([]));
  }

  relationFiltersFormArray(): FormArray {
      return this.relationFiltersFormGroup.get('relationFilters') as FormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(filters: Array<RelationEntityTypeFilter>): void {
    if (this.valueChangeSubscription) {
      this.valueChangeSubscription.unsubscribe();
    }
    const relationFiltersControls: Array<AbstractControl> = [];
    if (filters && filters.length) {
      filters.forEach((filter) => {
        relationFiltersControls.push(this.createRelationFilterFormGroup(filter));
      });
    }
    this.relationFiltersFormGroup.setControl('relationFilters', this.fb.array(relationFiltersControls));
    this.valueChangeSubscription = this.relationFiltersFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
  }

  public removeFilter(index: number) {
    (this.relationFiltersFormGroup.get('relationFilters') as FormArray).removeAt(index);
  }

  public addFilter() {
    const relationFiltersFormArray = this.relationFiltersFormGroup.get('relationFilters') as FormArray;
    const filter: RelationEntityTypeFilter = {
      relationType: null,
      entityTypes: []
    };
    relationFiltersFormArray.push(this.createRelationFilterFormGroup(filter));
  }

  private createRelationFilterFormGroup(filter: RelationEntityTypeFilter): AbstractControl {
    return this.fb.group({
      relationType: [filter ? filter.relationType : null],
      entityTypes: [filter ? filter.entityTypes : []]
    });
  }

  private updateModel() {
    const filters: Array<RelationEntityTypeFilter> = this.relationFiltersFormGroup.get('relationFilters').value;
    this.propagateChange(filters);
  }
}
