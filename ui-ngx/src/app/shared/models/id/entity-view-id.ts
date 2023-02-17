import {EntityId} from './entity-id';
import {EntityType} from '@shared/models/entity-type.models';

export class EntityViewId implements EntityId {
  entityType = EntityType.ENTITY_VIEW;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
