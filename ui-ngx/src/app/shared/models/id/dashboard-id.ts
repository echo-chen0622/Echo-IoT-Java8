import {EntityId} from './entity-id';
import {EntityType} from '@shared/models/entity-type.models';

export class DashboardId implements EntityId {
  entityType = EntityType.DASHBOARD;
  id: string;
  constructor(id: string) {
    this.id = id;
  }
}
