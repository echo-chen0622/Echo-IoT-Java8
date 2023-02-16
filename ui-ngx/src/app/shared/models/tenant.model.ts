///
/// Copyright © 2016-2023 The Echoiot Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { ContactBased } from '@shared/models/contact-based.model';
import { TenantId } from './id/tenant-id';
import { TenantProfileId } from '@shared/models/id/tenant-profile-id';
import { BaseData } from '@shared/models/base-data';
import { QueueInfo } from '@shared/models/queue.models';

export enum TenantProfileType {
  DEFAULT = 'DEFAULT'
}

export interface DefaultTenantProfileConfiguration {
  maxDevices: number;
  maxAssets: number;
  maxCustomers: number;
  maxUsers: number;
  maxDashboards: number;
  maxRuleChains: number;
  maxResourcesInBytes: number;
  maxOtaPackagesInBytes: number;

  transportTenantMsgRateLimit?: string;
  transportTenantTelemetryMsgRateLimit?: string;
  transportTenantTelemetryDataPointsRateLimit?: string;
  transportDeviceMsgRateLimit?: string;
  transportDeviceTelemetryMsgRateLimit?: string;
  transportDeviceTelemetryDataPointsRateLimit?: string;

  tenantEntityExportRateLimit?: string;
  tenantEntityImportRateLimit?: string;

  maxTransportMessages: number;
  maxTransportDataPoints: number;
  maxREExecutions: number;
  maxJSExecutions: number;
  maxDPStorageDays: number;
  maxRuleNodeExecutionsPerMessage: number;
  maxEmails: number;
  maxSms: number;
  maxCreatedAlarms: number;

  tenantServerRestLimitsConfiguration: string;
  customerServerRestLimitsConfiguration: string;

  maxWsSessionsPerTenant: number;
  maxWsSessionsPerCustomer: number;
  maxWsSessionsPerRegularUser: number;
  maxWsSessionsPerPublicUser: number;
  wsMsgQueueLimitPerSession: number;
  maxWsSubscriptionsPerTenant: number;
  maxWsSubscriptionsPerCustomer: number;
  maxWsSubscriptionsPerRegularUser: number;
  maxWsSubscriptionsPerPublicUser: number;
  wsUpdatesPerSessionRateLimit: string;

  cassandraQueryTenantRateLimitsConfiguration: string;

  defaultStorageTtlDays: number;
  alarmsTtlDays: number;
  rpcTtlDays: number;
}

export type TenantProfileConfigurations = DefaultTenantProfileConfiguration;

export interface TenantProfileConfiguration extends TenantProfileConfigurations {
  type: TenantProfileType;
}

export function createTenantProfileConfiguration(type: TenantProfileType): TenantProfileConfiguration {
  let configuration: TenantProfileConfiguration = null;
  if (type) {
    switch (type) {
      case TenantProfileType.DEFAULT:
        const defaultConfiguration: DefaultTenantProfileConfiguration = {
          maxDevices: 0,
          maxAssets: 0,
          maxCustomers: 0,
          maxUsers: 0,
          maxDashboards: 0,
          maxRuleChains: 0,
          maxResourcesInBytes: 0,
          maxOtaPackagesInBytes: 0,
          maxTransportMessages: 0,
          maxTransportDataPoints: 0,
          maxREExecutions: 0,
          maxJSExecutions: 0,
          maxDPStorageDays: 0,
          maxRuleNodeExecutionsPerMessage: 0,
          maxEmails: 0,
          maxSms: 0,
          maxCreatedAlarms: 0,
          tenantServerRestLimitsConfiguration: '',
          customerServerRestLimitsConfiguration: '',
          maxWsSessionsPerTenant: 0,
          maxWsSessionsPerCustomer: 0,
          maxWsSessionsPerRegularUser: 0,
          maxWsSessionsPerPublicUser: 0,
          wsMsgQueueLimitPerSession: 0,
          maxWsSubscriptionsPerTenant: 0,
          maxWsSubscriptionsPerCustomer: 0,
          maxWsSubscriptionsPerRegularUser: 0,
          maxWsSubscriptionsPerPublicUser: 0,
          wsUpdatesPerSessionRateLimit: '',
          cassandraQueryTenantRateLimitsConfiguration: '',
          defaultStorageTtlDays: 0,
          alarmsTtlDays: 0,
          rpcTtlDays: 0,
        };
        configuration = {...defaultConfiguration, type: TenantProfileType.DEFAULT};
        break;
    }
  }
  return configuration;
}

export interface TenantProfileData {
  configuration: TenantProfileConfiguration;
  queueConfiguration?: Array<QueueInfo>;
}

export interface TenantProfile extends BaseData<TenantProfileId> {
  name: string;
  description?: string;
  default?: boolean;
  isolatedTbRuleEngine?: boolean;
  profileData?: TenantProfileData;
}

export interface Tenant extends ContactBased<TenantId> {
  title: string;
  region: string;
  tenantProfileId: TenantProfileId;
  additionalInfo?: any;
}

export interface TenantInfo extends Tenant {
  tenantProfileName: string;
}
