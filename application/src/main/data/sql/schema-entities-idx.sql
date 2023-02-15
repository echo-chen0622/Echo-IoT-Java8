--
-- Copyright © 2016-2023 The Thingsboard Authors
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

CREATE INDEX IF NOT EXISTS idx_alarm_originator_alarm_type ON alarm(originator_id, type, start_ts DESC);

CREATE INDEX IF NOT EXISTS idx_alarm_originator_created_time ON alarm(originator_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_alarm_tenant_created_time ON alarm(tenant_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_alarm_tenant_status_created_time ON alarm(tenant_id, status, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_alarm_tenant_alarm_type_created_time ON alarm(tenant_id, type, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_entity_alarm_created_time ON entity_alarm(tenant_id, entity_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_entity_alarm_alarm_id ON entity_alarm(alarm_id);

CREATE INDEX IF NOT EXISTS idx_relation_to_id ON relation(relation_type_group, to_type, to_id);

CREATE INDEX IF NOT EXISTS idx_relation_from_id ON relation(relation_type_group, from_type, from_id);

CREATE INDEX IF NOT EXISTS idx_device_customer_id ON device(tenant_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_device_customer_id_and_type ON device(tenant_id, customer_id, type);

CREATE INDEX IF NOT EXISTS idx_device_type ON device(tenant_id, type);

CREATE INDEX IF NOT EXISTS idx_device_device_profile_id ON device(tenant_id, device_profile_id);

CREATE INDEX IF NOT EXISTS idx_asset_customer_id ON asset(tenant_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_asset_customer_id_and_type ON asset(tenant_id, customer_id, type);

CREATE INDEX IF NOT EXISTS idx_asset_type ON asset(tenant_id, type);

CREATE INDEX IF NOT EXISTS idx_attribute_kv_by_key_and_last_update_ts ON attribute_kv(entity_id, attribute_key, last_update_ts desc);

CREATE INDEX IF NOT EXISTS idx_audit_log_tenant_id_and_created_time ON audit_log(tenant_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_audit_log_id ON audit_log(id);

CREATE INDEX IF NOT EXISTS idx_edge_event_tenant_id_and_created_time ON edge_event(tenant_id, created_time DESC);

CREATE INDEX IF NOT EXISTS idx_edge_event_id ON edge_event(id);

CREATE INDEX IF NOT EXISTS idx_rpc_tenant_id_device_id ON rpc(tenant_id, device_id);

CREATE INDEX IF NOT EXISTS idx_device_external_id ON device(tenant_id, external_id);

CREATE INDEX IF NOT EXISTS idx_device_profile_external_id ON device_profile(tenant_id, external_id);

CREATE INDEX IF NOT EXISTS idx_asset_external_id ON asset(tenant_id, external_id);

CREATE INDEX IF NOT EXISTS idx_entity_view_external_id ON entity_view(tenant_id, external_id);

CREATE INDEX IF NOT EXISTS idx_rule_chain_external_id ON rule_chain(tenant_id, external_id);

CREATE INDEX IF NOT EXISTS idx_dashboard_external_id ON dashboard(tenant_id, external_id);

CREATE INDEX IF NOT EXISTS idx_customer_external_id ON customer(tenant_id, external_id);

CREATE INDEX IF NOT EXISTS idx_widgets_bundle_external_id ON widgets_bundle(tenant_id, external_id);

CREATE INDEX IF NOT EXISTS idx_rule_node_external_id ON rule_node(rule_chain_id, external_id);

CREATE INDEX IF NOT EXISTS idx_rule_node_type ON rule_node(type);

CREATE INDEX IF NOT EXISTS idx_api_usage_state_entity_id ON api_usage_state(entity_id);
