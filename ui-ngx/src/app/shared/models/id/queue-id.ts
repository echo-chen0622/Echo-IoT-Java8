import { EntityId } from './entity-id';
import { EntityType } from '@shared/models/entity-type.models';

export class QueueId implements EntityId {
  entityType = EntityType.QUEUE;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
