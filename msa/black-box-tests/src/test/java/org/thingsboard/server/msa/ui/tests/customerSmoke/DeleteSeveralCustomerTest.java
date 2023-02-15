package org.thingsboard.server.msa.ui.tests.customerSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultCustomerPrototype;

public class DeleteSeveralCustomerTest extends AbstractDriverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private CustomerPageHelper customerPage;

    @BeforeMethod
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        customerPage = new CustomerPageHelper(driver);
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void canDeleteSeveralCustomersByTopBtn() {
        String title1 = ENTITY_NAME + random() + "1";
        String title2 = ENTITY_NAME + random() + "2";
        testRestClient.postCustomer(defaultCustomerPrototype(title1));
        testRestClient.postCustomer(defaultCustomerPrototype(title2));

        sideBarMenuView.customerBtn().click();
        customerPage.clickOnCheckBoxes(2);
        customerPage.deleteSelectedBtn().click();
        customerPage.warningPopUpYesBtn().click();
        customerPage.refreshBtn().click();

        Assert.assertTrue(customerPage.customerIsNotPresent(title1));
        Assert.assertTrue(customerPage.customerIsNotPresent(title2));
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void selectAllCustomers() {
        sideBarMenuView.customerBtn().click();
        customerPage.selectAllCheckBox().click();
        customerPage.deleteSelectedBtn().click();

        Assert.assertNotNull(customerPage.warningPopUpTitle());
        Assert.assertTrue(customerPage.warningPopUpTitle().isDisplayed());
        Assert.assertTrue(customerPage.warningPopUpTitle().getText().contains(String.valueOf(customerPage.markCheckbox().size())));
    }

    @Test(priority = 30, groups = "smoke")
    @Description
    public void deleteSeveralCustomersByTopBtnWithoutRefresh() {
        String title1 = ENTITY_NAME + random() + "1";
        String title2 = ENTITY_NAME + random() + "2";
        testRestClient.postCustomer(defaultCustomerPrototype(title1));
        testRestClient.postCustomer(defaultCustomerPrototype(title2));

        sideBarMenuView.customerBtn().click();
        customerPage.clickOnCheckBoxes(2);
        customerPage.deleteSelectedBtn().click();
        customerPage.warningPopUpYesBtn().click();

        Assert.assertTrue(customerPage.customerIsNotPresent(title1));
        Assert.assertTrue(customerPage.customerIsNotPresent(title2));
    }
}
