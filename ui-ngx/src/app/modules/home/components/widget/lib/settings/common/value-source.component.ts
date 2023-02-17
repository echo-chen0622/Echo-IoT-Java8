import { Component, ElementRef, forwardRef, HostBinding, Input, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormBuilder, FormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { TranslateService } from '@ngx-translate/core';
import { IAliasController } from '@core/api/widget-api.models';
import { Observable, of } from 'rxjs';
import { catchError, map, mergeMap, publishReplay, refCount, tap } from 'rxjs/operators';
import { DataKey } from '@shared/models/widget.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { EntityService } from '@core/http/entity.service';

export declare type ValueSource = 'predefinedValue' | 'entityAttribute';

export interface ValueSourceProperty {
  valueSource: ValueSource;
  entityAlias?: string;
  attribute?: string;
  value?: number;
}

@Component({
  selector: 'tb-value-source',
  templateUrl: './value-source.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ValueSourceComponent),
      multi: true
    }
  ]
})
export class ValueSourceComponent extends PageComponent implements OnInit, ControlValueAccessor {

  @HostBinding('style.display') display = 'block';

  @ViewChild('entityAliasInput') entityAliasInput: ElementRef;

  @ViewChild('keyInput') keyInput: ElementRef;

  @Input()
  disabled: boolean;

  @Input()
  aliasController: IAliasController;

  private modelValue: ValueSourceProperty;

  private propagateChange = null;

  public valueSourceFormGroup: FormGroup;

  filteredEntityAliases: Observable<Array<string>>;
  aliasSearchText = '';

  filteredKeys: Observable<Array<string>>;
  keySearchText = '';

  private latestKeySearchResult: Array<string> = null;
  private keysFetchObservable$: Observable<Array<string>> = null;

  private entityAliasList: Array<string> = [];

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private entityService: EntityService,
              private fb: FormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.valueSourceFormGroup = this.fb.group({
      valueSource: ['predefinedValue', []],
      entityAlias: [null, []],
      attribute: [null, []],
      value: [null, []]
    });
    this.valueSourceFormGroup.valueChanges.subscribe(() => {
      this.updateModel();
    });
    this.valueSourceFormGroup.get('valueSource').valueChanges.subscribe(() => {
      this.updateValidators(true);
    });
    this.updateValidators(false);

    this.filteredEntityAliases = this.valueSourceFormGroup.get('entityAlias').valueChanges
      .pipe(
        tap(() => {
          this.latestKeySearchResult = null;
          this.keysFetchObservable$ = null;
          this.valueSourceFormGroup.get('attribute').setValue(this.valueSourceFormGroup.get('attribute').value);
        }),
        map(value => value ? value : ''),
        mergeMap(name => this.fetchEntityAliases(name) )
    );

    this.filteredKeys = this.valueSourceFormGroup.get('attribute').valueChanges
      .pipe(
        map(value => value ? value : ''),
        mergeMap(name => this.fetchKeys(name) )
    );

    if (this.aliasController) {
      const entityAliases = this.aliasController.getEntityAliases();
      for (const aliasId of Object.keys(entityAliases)) {
        this.entityAliasList.push(entityAliases[aliasId].alias);
      }
    }
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (isDisabled) {
      this.valueSourceFormGroup.disable({emitEvent: false});
    } else {
      this.valueSourceFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: ValueSourceProperty): void {
    this.modelValue = value;
    this.valueSourceFormGroup.patchValue(
      value, {emitEvent: false}
    );
    this.updateValidators(false);
  }

  clearEntityAlias() {
    this.valueSourceFormGroup.get('entityAlias').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.entityAliasInput.nativeElement.blur();
      this.entityAliasInput.nativeElement.focus();
    }, 0);
  }

  clearKey() {
    this.valueSourceFormGroup.get('attribute').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.keyInput.nativeElement.blur();
      this.keyInput.nativeElement.focus();
    }, 0);
  }

  private fetchEntityAliases(searchText?: string): Observable<Array<string>> {
    this.aliasSearchText = searchText;
    let result = this.entityAliasList;
    if (searchText && searchText.length) {
      result = this.entityAliasList.filter((entityAlias) => entityAlias.toLowerCase().includes(searchText.toLowerCase()));
    }
    return of(result);
  }

  private fetchKeys(searchText?: string): Observable<Array<string>> {
    if (this.keySearchText !== searchText || this.latestKeySearchResult === null) {
      this.keySearchText = searchText;
      const dataKeyFilter = this.createKeyFilter(this.keySearchText);
      return this.getKeys().pipe(
        map(name => name.filter(dataKeyFilter)),
        tap(res => this.latestKeySearchResult = res)
      );
    }
    return of(this.latestKeySearchResult);
  }

  private getKeys() {
    if (this.keysFetchObservable$ === null) {
      let fetchObservable: Observable<Array<DataKey>>;
      let entityAliasId: string;
      const entityAlias: string = this.valueSourceFormGroup.get('entityAlias').value;
      if (entityAlias && this.aliasController) {
        entityAliasId = this.aliasController.getEntityAliasId(entityAlias);
      }
      if (entityAliasId) {
        const dataKeyTypes = [DataKeyType.attribute];
        fetchObservable = this.fetchEntityKeys(entityAliasId, dataKeyTypes);
      } else {
        fetchObservable = of([]);
      }
      this.keysFetchObservable$ = fetchObservable.pipe(
        map((dataKeys) => dataKeys.map((dataKey) => dataKey.name)),
        publishReplay(1),
        refCount()
      );
    }
    return this.keysFetchObservable$;
  }

  private fetchEntityKeys(entityAliasId: string, dataKeyTypes: Array<DataKeyType>): Observable<Array<DataKey>> {
    return this.aliasController.getAliasInfo(entityAliasId).pipe(
      mergeMap((aliasInfo) => {
        return this.entityService.getEntityKeysByEntityFilter(
          aliasInfo.entityFilter,
          dataKeyTypes,
          {ignoreLoading: true, ignoreErrors: true}
        ).pipe(
          catchError(() => of([]))
        );
      }),
      catchError(() => of([] as Array<DataKey>))
    );
  }

  private createKeyFilter(query: string): (key: string) => boolean {
    const lowercaseQuery = query.toLowerCase();
    return key => key.toLowerCase().startsWith(lowercaseQuery);
  }

  private updateModel() {
    const value: ValueSourceProperty = this.valueSourceFormGroup.value;
    this.modelValue = value;
    this.propagateChange(this.modelValue);
  }

  private updateValidators(emitEvent?: boolean): void {
    const valueSource: ValueSource = this.valueSourceFormGroup.get('valueSource').value;
    if (valueSource === 'predefinedValue') {
      this.valueSourceFormGroup.get('entityAlias').disable({emitEvent});
      this.valueSourceFormGroup.get('attribute').disable({emitEvent});
      this.valueSourceFormGroup.get('value').enable({emitEvent});
    } else if (valueSource === 'entityAttribute') {
      this.valueSourceFormGroup.get('entityAlias').enable({emitEvent});
      this.valueSourceFormGroup.get('attribute').enable({emitEvent});
      this.valueSourceFormGroup.get('value').disable({emitEvent});
    }
    this.valueSourceFormGroup.get('entityAlias').updateValueAndValidity({emitEvent: false});
    this.valueSourceFormGroup.get('attribute').updateValueAndValidity({emitEvent: false});
    this.valueSourceFormGroup.get('value').updateValueAndValidity({emitEvent: false});
  }

}
