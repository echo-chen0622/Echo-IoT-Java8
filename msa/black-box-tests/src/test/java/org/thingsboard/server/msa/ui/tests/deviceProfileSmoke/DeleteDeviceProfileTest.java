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
package org.thingsboard.server.msa.ui.tests.deviceProfileSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.ProfilesPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

public class DeleteDeviceProfileTest extends AbstractDriverBaseTest {

    private SideBarMenuViewHelper sideBarMenuView;
    private ProfilesPageHelper profilesPage;

    @BeforeMethod
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        profilesPage = new ProfilesPageHelper(driver);
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void removeDeviceProfile() {
        String name = ENTITY_NAME + random();
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name));

        sideBarMenuView.openDeviceProfiles();
        profilesPage.deleteBtn(name).click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn();

        Assert.assertTrue(profilesPage.entityIsNotPresent(name));
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void removeDeviceProfileFromView() {
        String name = ENTITY_NAME + random();
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name));

        sideBarMenuView.openDeviceProfiles();
        profilesPage.entity(name).click();
        profilesPage.deviceProfileViewDeleteBtn().click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn();

        Assert.assertTrue(profilesPage.entityIsNotPresent(name));
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void removeSelectedDeviceProfile() {
        String name = ENTITY_NAME + random();
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name));

        sideBarMenuView.openDeviceProfiles();
        profilesPage.checkBox(name).click();
        profilesPage.deleteSelectedBtn().click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn();

        Assert.assertTrue(profilesPage.entityIsNotPresent(name));
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void removeDefaultDeviceProfile() {
        sideBarMenuView.openDeviceProfiles();

        Assert.assertFalse(profilesPage.deleteBtn("default").isEnabled());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void removeDefaultDeviceProfileFromView() {
        sideBarMenuView.openDeviceProfiles();
        profilesPage.entity("default").click();

        Assert.assertTrue(profilesPage.deleteDeviceProfileFromViewBtnIsNotDisplayed());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void removeSelectedDefaultDeviceProfile() {
        sideBarMenuView.openDeviceProfiles();

        Assert.assertNotNull(profilesPage.presentCheckBox("default"));
        Assert.assertFalse(profilesPage.presentCheckBox("default").isDisplayed());
    }

    @Test(priority = 30, groups = "smoke")
    @Description
    public void removeDeviceProfileWithoutRefresh() {
        String name = ENTITY_NAME + random();
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name));

        sideBarMenuView.openDeviceProfiles();
        profilesPage.deleteBtn(name).click();
        profilesPage.warningPopUpYesBtn().click();

        Assert.assertTrue(profilesPage.entityIsNotPresent(name));
    }
}
