package org.thingsboard.server.msa.ui.tests.assetProfileSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.ProfilesPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

public class MakeAssetProfileDefaultTest extends AbstractDriverBaseTest {
    private SideBarMenuViewHelper sideBarMenuView;
    private ProfilesPageHelper profilesPage;
    private String name;

    @BeforeMethod
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        profilesPage = new ProfilesPageHelper(driver);
    }

    @AfterMethod
    public void makeProfileDefault() {
        testRestClient.setDefaultAssetProfile(getAssetProfileByName("default").getId());
        testRestClient.deleteAssetProfile(getAssetProfileByName(name).getId());
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void makeDeviceProfileDefaultByRightCornerBtn() {
        String name = ENTITY_NAME + random();
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name));
        this.name = name;

        sideBarMenuView.openAssetProfiles();
        profilesPage.makeProfileDefaultBtn(name).click();
        profilesPage.warningPopUpYesBtn().click();

        Assert.assertTrue(profilesPage.defaultCheckbox(name).isDisplayed());
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void makeDeviceProfileDefaultFromView() {
        String name = ENTITY_NAME + random();
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name));
        this.name = name;

        sideBarMenuView.openAssetProfiles();
        profilesPage.entity(name).click();
        profilesPage.assetProfileViewMakeDefaultBtn().click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.closeEntityViewBtn().click();

        Assert.assertTrue(profilesPage.defaultCheckbox(name).isDisplayed());
    }
}
