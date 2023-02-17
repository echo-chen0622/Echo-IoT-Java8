import { EntityId } from './entity-id';
import { EntityType } from '@shared/models/entity-type.models';

export class RuleChainId implements EntityId {
  entityType = EntityType.RULE_CHAIN;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
