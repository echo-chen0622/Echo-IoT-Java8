import { EntityId } from '@shared/models/id/entity-id';
import { EntityType } from '@shared/models/entity-type.models';

export class OtaPackageId implements EntityId {
  entityType = EntityType.OTA_PACKAGE;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
