import {EntityId} from '@shared/models/id/entity-id';
import {HasUUID} from '@shared/models/id/has-uuid';
import {isDefinedAndNotNull} from '@core/utils';

export declare type HasId = EntityId | HasUUID;

export interface BaseData<T extends HasId> {
  createdTime?: number;
  id?: T;
  name?: string;
  label?: string;
}

export interface ExportableEntity<T extends EntityId> {
  createdTime?: number;
  id?: T;
  externalId?: T;
}

export function hasIdEquals(id1: HasId, id2: HasId): boolean {
  if (isDefinedAndNotNull(id1) && isDefinedAndNotNull(id2)) {
    return id1.id === id2.id;
  } else {
    return id1 === id2;
  }
}
