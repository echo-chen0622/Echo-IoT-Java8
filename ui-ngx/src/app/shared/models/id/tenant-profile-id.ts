import { EntityId } from './entity-id';
import { EntityType } from '@shared/models/entity-type.models';

export class TenantProfileId implements EntityId {
  entityType = EntityType.TENANT_PROFILE;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
