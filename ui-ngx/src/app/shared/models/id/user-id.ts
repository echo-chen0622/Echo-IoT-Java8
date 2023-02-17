import { EntityId } from './entity-id';
import { EntityType } from '@shared/models/entity-type.models';

export class UserId implements EntityId {
  entityType = EntityType.USER;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
