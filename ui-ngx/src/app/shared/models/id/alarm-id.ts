import {EntityId} from './entity-id';
import {EntityType} from '@shared/models/entity-type.models';

export class AlarmId implements EntityId {
  entityType = EntityType.ALARM;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
