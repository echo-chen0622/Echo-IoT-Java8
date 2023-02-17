import { EntityId } from './entity-id';
import { EntityType } from '@shared/models/entity-type.models';

export class WidgetsBundleId implements EntityId {
  entityType = EntityType.WIDGETS_BUNDLE;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
