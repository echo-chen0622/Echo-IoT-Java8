package org.thingsboard.server.msa.ui.tests.deviceProfileSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.ProfilesPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;

import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultDeviceProfile;

public class SortByNameTest extends AbstractDriverBaseTest {
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
    public void delete() {
        if (name != null) {
            testRestClient.deleteDeviseProfile(getDeviceProfileByName(name).getId());
            name = null;
        }
    }

    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description
    public void specialCharacterUp(String name) {
        testRestClient.postDeviceProfile(defaultDeviceProfile(name));
        this.name = name;

        sideBarMenuView.openDeviceProfiles();
        profilesPage.sortByNameBtn().click();
        profilesPage.setProfileName();

        Assert.assertEquals(profilesPage.getProfileName(), name);
    }

    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description
    public void allSortUp(String deviceProfile, String deviceProfileSymbol, String deviceProfileNumber) {
        testRestClient.postDeviceProfile(defaultDeviceProfile(deviceProfileSymbol));
        testRestClient.postDeviceProfile(defaultDeviceProfile(deviceProfile));
        testRestClient.postDeviceProfile(defaultDeviceProfile(deviceProfileNumber));

        sideBarMenuView.openDeviceProfiles();
        profilesPage.sortByNameBtn().click();
        profilesPage.setProfileName(0);
        String firstDeviceProfile = profilesPage.getProfileName();
        profilesPage.setProfileName(1);
        String secondDeviceProfile = profilesPage.getProfileName();
        profilesPage.setProfileName(2);
        String thirdDeviceProfile = profilesPage.getProfileName();

        testRestClient.deleteDeviseProfile(getDeviceProfileByName(deviceProfile).getId());
        testRestClient.deleteDeviseProfile(getDeviceProfileByName(deviceProfileNumber).getId());
        testRestClient.deleteDeviseProfile(getDeviceProfileByName(deviceProfileSymbol).getId());

        Assert.assertEquals(firstDeviceProfile, deviceProfileSymbol);
        Assert.assertEquals(secondDeviceProfile, deviceProfileNumber);
        Assert.assertEquals(thirdDeviceProfile, deviceProfile);
    }

    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description
    public void specialCharacterDown(String name) {
        testRestClient.postDeviceProfile(defaultDeviceProfile(name));
        this.name = name;

        sideBarMenuView.openDeviceProfiles();
        profilesPage.sortByNameDown();
        profilesPage.setProfileName(profilesPage.allEntity().size() - 1);

        Assert.assertEquals(profilesPage.getProfileName(), name);
    }

    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description
    public void allSortDown(String deviceProfile, String deviceProfileSymbol, String deviceProfileNumber) {
        testRestClient.postDeviceProfile(defaultDeviceProfile(deviceProfileSymbol));
        testRestClient.postDeviceProfile(defaultDeviceProfile(deviceProfile));
        testRestClient.postDeviceProfile(defaultDeviceProfile(deviceProfileNumber));

        sideBarMenuView.openDeviceProfiles();
        int lastIndex = profilesPage.allEntity().size() - 1;
        profilesPage.sortByNameDown();
        profilesPage.setProfileName(lastIndex);
        String firstDeviceProfile = profilesPage.getProfileName();
        profilesPage.setProfileName(lastIndex - 1);
        String secondDeviceProfile = profilesPage.getProfileName();
        profilesPage.setProfileName(lastIndex - 2);
        String thirdDeviceProfile = profilesPage.getProfileName();

        testRestClient.deleteDeviseProfile(getDeviceProfileByName(deviceProfile).getId());
        testRestClient.deleteDeviseProfile(getDeviceProfileByName(deviceProfileNumber).getId());
        testRestClient.deleteDeviseProfile(getDeviceProfileByName(deviceProfileSymbol).getId());

        Assert.assertEquals(firstDeviceProfile, deviceProfileSymbol);
        Assert.assertEquals(secondDeviceProfile, deviceProfileNumber);
        Assert.assertEquals(thirdDeviceProfile, deviceProfile);
    }
}
