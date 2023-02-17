import {EntityId} from './entity-id';
import {EntityType} from '@shared/models/entity-type.models';

export class AssetId implements EntityId {
  entityType = EntityType.ASSET;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
