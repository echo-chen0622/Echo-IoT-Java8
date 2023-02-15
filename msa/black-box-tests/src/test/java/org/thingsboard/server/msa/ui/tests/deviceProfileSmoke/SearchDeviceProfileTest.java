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

public class SearchDeviceProfileTest extends AbstractDriverBaseTest {

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

    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "deviceProfileSearch")
    @Description
    public void searchFirstWord(String name, String namePath) {
        testRestClient.postDeviceProfile(defaultDeviceProfile(name));
        this.name = name;

        sideBarMenuView.openDeviceProfiles();
        profilesPage.searchEntity(namePath);

        profilesPage.allEntity().forEach(x -> Assert.assertTrue(x.getText().contains(namePath)));
    }
}
