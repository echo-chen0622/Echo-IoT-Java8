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

public class DeleteSeveralAssetProfilesTest extends AbstractDriverBaseTest {
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
    public void canDeleteSeveralAssetProfilesByTopBtn() {
        String name1 = ENTITY_NAME + random() + "1";
        String name2 = ENTITY_NAME + random() + "2";
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name1));
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name2));

        sideBarMenuView.openAssetProfiles();
        profilesPage.clickOnCheckBoxes(2);
        profilesPage.deleteSelectedBtn().click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn().click();

        Assert.assertTrue(profilesPage.profileIsNotPresent(name1));
        Assert.assertTrue(profilesPage.profileIsNotPresent(name2));
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void selectAllDAssetProfiles() {
        String name1 = ENTITY_NAME + random() + "1";
        String name2 = ENTITY_NAME + random() +"2";
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name1));
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name2));

        sideBarMenuView.openAssetProfiles();
        profilesPage.selectAllCheckBox().click();
        profilesPage.deleteSelectedBtn().click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn().click();

        Assert.assertTrue(profilesPage.profileIsNotPresent(name1));
        Assert.assertTrue(profilesPage.profileIsNotPresent(name2));
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void removeDefaultAssetProfile() {
        sideBarMenuView.openAssetProfiles();
        profilesPage.selectAllCheckBox().click();

        Assert.assertFalse(profilesPage.checkBoxIsDisplayed("default"));
        Assert.assertFalse(profilesPage.deleteBtn("default").isEnabled());
    }

    @Test(priority = 30, groups = "smoke")
    @Description
    public void deleteSeveralAssetProfilesByTopBtnWithoutRefresh() {
        String name1 = ENTITY_NAME + random() + "1";
        String name2 = ENTITY_NAME + random() + "2";
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name1));
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name2));

        sideBarMenuView.openAssetProfiles();
        profilesPage.clickOnCheckBoxes(2);
        profilesPage.deleteSelectedBtn().click();
        profilesPage.warningPopUpYesBtn().click();

        Assert.assertTrue(profilesPage.profileIsNotPresent(name1));
        Assert.assertTrue(profilesPage.profileIsNotPresent(name2));
    }
}
