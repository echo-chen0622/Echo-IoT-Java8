import { EntityId } from './entity-id';
import { EntityType } from '@shared/models/entity-type.models';

export class TenantId implements EntityId {
  entityType = EntityType.TENANT;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
