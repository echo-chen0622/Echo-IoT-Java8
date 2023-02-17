import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';

export class RpcId implements EntityId {
  entityType = EntityType.RPC;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
