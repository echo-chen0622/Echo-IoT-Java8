/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.service;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.id.TenantId;


public abstract class BaseApiUsageStateServiceTest extends AbstractServiceTest {

    private TenantId tenantId;

    @Before
    public void before() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindApiUsageStateByTenantId() {
        ApiUsageState apiUsageState = apiUsageStateService.findTenantApiUsageState(tenantId);
        Assert.assertNotNull(apiUsageState);
    }

    @Test
    public void testUpdateApiUsageState(){
        ApiUsageState apiUsageState = apiUsageStateService.findTenantApiUsageState(tenantId);
        Assert.assertNotNull(apiUsageState);
        Assert.assertTrue(apiUsageState.isTransportEnabled());
        apiUsageState.setTransportState(ApiUsageStateValue.DISABLED);
        apiUsageState = apiUsageStateService.update(apiUsageState);
        Assert.assertNotNull(apiUsageState);
        apiUsageState = apiUsageStateService.findTenantApiUsageState(tenantId);
        Assert.assertNotNull(apiUsageState);
        Assert.assertFalse(apiUsageState.isTransportEnabled());
    }

}
