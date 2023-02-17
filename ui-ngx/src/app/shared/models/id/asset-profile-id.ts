import {EntityId} from './entity-id';
import {EntityType} from '@shared/models/entity-type.models';

export class AssetProfileId implements EntityId {
  entityType = EntityType.ASSET_PROFILE;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
