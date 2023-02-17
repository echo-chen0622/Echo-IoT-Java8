import { EntityId } from './entity-id';
import { EntityType } from '@shared/models/entity-type.models';

export class WidgetTypeId implements EntityId {
  entityType = EntityType.WIDGET_TYPE;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
