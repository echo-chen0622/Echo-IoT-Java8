package org.thingsboard.server.msa.ui.tests.assetProfileSmoke;

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

public class DeleteAssetProfileTest extends AbstractDriverBaseTest {

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
    public void removeAssetProfile() {
        String name = ENTITY_NAME + random();
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name));

        sideBarMenuView.openAssetProfiles();
        profilesPage.deleteBtn(name).click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn();

        Assert.assertTrue(profilesPage.entityIsNotPresent(name));
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void removeAssetProfileFromView() {
        String name = ENTITY_NAME + random();
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name));

        sideBarMenuView.openAssetProfiles();
        profilesPage.entity(name).click();
        profilesPage.assetProfileViewDeleteBtn().click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn();

        Assert.assertTrue(profilesPage.entityIsNotPresent(name));
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void removeSelectedAssetProfile() {
        String name = ENTITY_NAME + random();
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name));

        sideBarMenuView.openAssetProfiles();
        profilesPage.checkBox(name).click();
        profilesPage.deleteSelectedBtn().click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn();

        Assert.assertTrue(profilesPage.entityIsNotPresent(name));
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void removeDefaultAssetProfile() {
        sideBarMenuView.openAssetProfiles();

        Assert.assertFalse(profilesPage.deleteBtn("default").isEnabled());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void removeDefaultAssetProfileFromView() {
        sideBarMenuView.openAssetProfiles();
        profilesPage.entity("default").click();

        Assert.assertTrue(profilesPage.deleteAssetProfileFromViewBtnIsNotDisplayed());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void removeSelectedDefaultAssetProfile() {
        sideBarMenuView.openAssetProfiles();

        Assert.assertNotNull(profilesPage.presentCheckBox("default"));
        Assert.assertFalse(profilesPage.presentCheckBox("default").isDisplayed());
    }

    @Test(priority = 30, groups = "smoke")
    @Description
    public void removeAssetProfileWithoutRefresh() {
        String name = ENTITY_NAME + random();
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name));

        sideBarMenuView.openAssetProfiles();
        profilesPage.deleteBtn(name).click();
        profilesPage.warningPopUpYesBtn().click();

        Assert.assertTrue(profilesPage.entityIsNotPresent(name));
    }
}
