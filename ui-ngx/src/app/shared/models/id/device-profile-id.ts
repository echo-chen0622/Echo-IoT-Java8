import {EntityId} from './entity-id';
import {EntityType} from '@shared/models/entity-type.models';

export class DeviceProfileId implements EntityId {
  entityType = EntityType.DEVICE_PROFILE;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
