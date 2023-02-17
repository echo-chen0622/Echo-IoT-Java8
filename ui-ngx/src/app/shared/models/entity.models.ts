import { EntityType } from '@shared/models/entity-type.models';
import { AttributeData } from './telemetry/telemetry.models';
import { EntityId } from '@shared/models/id/entity-id';
import { DeviceCredentialMQTTBasic } from '@shared/models/device.models';
import { Lwm2mSecurityConfigModels } from '@shared/models/lwm2m-security-config.models';

export interface EntityInfo {
  name?: string;
  label?: string;
  entityType?: EntityType;
  id?: string;
  entityDescription?: string;
}

export interface EntityInfoData {
  id: EntityId;
  name: string;
}

export interface ImportEntityData {
  lineNumber: number;
  name: string;
  type: string;
  label: string;
  gateway: boolean;
  description: string;
  credential: {
    accessToken?: string;
    x509?: string;
    mqtt?: DeviceCredentialMQTTBasic;
    lwm2m?: Lwm2mSecurityConfigModels;
  };
  attributes: {
    server: AttributeData[],
    shared: AttributeData[]
  };
  timeseries: AttributeData[];
}

export interface EdgeImportEntityData extends ImportEntityData {
  secret: string;
  routingKey: string;
}

export interface ImportEntitiesResultInfo {
  create?: {
    entity: number;
  };
  update?: {
    entity: number;
  };
  error?: {
    entity: number;
    errors?: string;
  };
}

export interface EntityField {
  keyName: string;
  value: string;
  name: string;
  time?: boolean;
}

export interface EntitiesKeysByQuery {
  attribute: Array<string>;
  timeseries: Array<string>;
  entityTypes: EntityType[];
}

export const entityFields: {[fieldName: string]: EntityField} = {
  createdTime: {
    keyName: 'createdTime',
    name: 'entity-field.created-time',
    value: 'createdTime',
    time: true
  },
  name: {
    keyName: 'name',
    name: 'entity-field.name',
    value: 'name'
  },
  type: {
    keyName: 'type',
    name: 'entity-field.type',
    value: 'type'
  },
  firstName: {
    keyName: 'firstName',
    name: 'entity-field.first-name',
    value: 'firstName'
  },
  lastName: {
    keyName: 'lastName',
    name: 'entity-field.last-name',
    value: 'lastName'
  },
  email: {
    keyName: 'email',
    name: 'entity-field.email',
    value: 'email'
  },
  title: {
    keyName: 'title',
    name: 'entity-field.title',
    value: 'title'
  },
  country: {
    keyName: 'country',
    name: 'entity-field.country',
    value: 'country'
  },
  state: {
    keyName: 'state',
    name: 'entity-field.state',
    value: 'state'
  },
  city: {
    keyName: 'city',
    name: 'entity-field.city',
    value: 'city'
  },
  address: {
    keyName: 'address',
    name: 'entity-field.address',
    value: 'address'
  },
  address2: {
    keyName: 'address2',
    name: 'entity-field.address2',
    value: 'address2'
  },
  zip: {
    keyName: 'zip',
    name: 'entity-field.zip',
    value: 'zip'
  },
  phone: {
    keyName: 'phone',
    name: 'entity-field.phone',
    value: 'phone'
  },
  label: {
    keyName: 'label',
    name: 'entity-field.label',
    value: 'label'
  }
};
