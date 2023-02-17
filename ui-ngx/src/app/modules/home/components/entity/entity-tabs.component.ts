import {BaseData, HasId} from '@shared/models/base-data';
import {PageComponent} from '@shared/components/page.component';
import {AfterViewInit, Directive, Input, OnInit, QueryList, ViewChildren} from '@angular/core';
import {Store} from '@ngrx/store';
import {AppState} from '@core/core.state';
import {EntityTableConfig} from '@home/models/entity/entities-table-config.models';
import {MatTab} from '@angular/material/tabs';
import {BehaviorSubject} from 'rxjs';
import {Authority} from '@app/shared/models/authority.enum';
import {getCurrentAuthUser} from '@core/auth/auth.selectors';
import {AuthUser} from '@shared/models/user.model';
import {EntityType} from '@shared/models/entity-type.models';
import {AuditLogMode} from '@shared/models/audit-log.models';
import {DebugEventType, EventType} from '@shared/models/event.models';
import {AttributeScope, LatestTelemetry} from '@shared/models/telemetry/telemetry.models';
import {NULL_UUID} from '@shared/models/id/has-uuid';
import {FormGroup} from '@angular/forms';
import {PageLink} from '@shared/models/page/page-link';

@Directive()
// tslint:disable-next-line:directive-class-suffix
export abstract class EntityTabsComponent<T extends BaseData<HasId>,
  P extends PageLink = PageLink,
  L extends BaseData<HasId> = T,
  C extends EntityTableConfig<T, P, L> = EntityTableConfig<T, P, L>>
  extends PageComponent implements OnInit, AfterViewInit {

  attributeScopes = AttributeScope;
  latestTelemetryTypes = LatestTelemetry;

  authorities = Authority;

  entityTypes = EntityType;

  auditLogModes = AuditLogMode;

  eventTypes = EventType;

  debugEventTypes = DebugEventType;

  authUser: AuthUser;

  nullUid = NULL_UUID;

  entityValue: T;

  entitiesTableConfigValue: C;

  @ViewChildren(MatTab) entityTabs: QueryList<MatTab>;

  isEditValue: boolean;

  @Input()
  set isEdit(isEdit: boolean) {
    this.isEditValue = isEdit;
  }

  get isEdit() {
    return this.isEditValue;
  }

  @Input()
  set entity(entity: T) {
    this.setEntity(entity);
  }

  get entity(): T {
    return this.entityValue;
  }

  @Input()
  set entitiesTableConfig(entitiesTableConfig: C) {
    this.setEntitiesTableConfig(entitiesTableConfig);
  }

  get entitiesTableConfig(): C {
    return this.entitiesTableConfigValue;
  }

  @Input()
  detailsForm: FormGroup;

  private entityTabsSubject = new BehaviorSubject<Array<MatTab>>(null);

  entityTabsChanged = this.entityTabsSubject.asObservable();

  protected constructor(protected store: Store<AppState>) {
    super(store);
    this.authUser = getCurrentAuthUser(store);
  }

  ngOnInit() {
  }

  ngAfterViewInit(): void {
    this.entityTabsSubject.next(this.entityTabs.toArray());
    this.entityTabs.changes.subscribe(
      () => {
        this.entityTabsSubject.next(this.entityTabs.toArray());
      }
    );
  }

  protected setEntity(entity: T) {
    this.entityValue = entity;
  }

  protected setEntitiesTableConfig(entitiesTableConfig: C) {
    this.entitiesTableConfigValue = entitiesTableConfig;
  }

}
