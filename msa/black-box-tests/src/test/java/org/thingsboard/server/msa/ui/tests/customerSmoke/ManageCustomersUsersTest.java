package org.thingsboard.server.msa.ui.tests.customerSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

public class ManageCustomersUsersTest extends AbstractDriverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private CustomerPageHelper customerPage;
    private final String iconText = "Customer Users";

    @BeforeMethod
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        customerPage = new CustomerPageHelper(driver);
    }

    @Test(groups = "smoke")
    @Description
    public void openWindowByRightCornerBtn() {
        sideBarMenuView.customerBtn().click();
        customerPage.setCustomerName();
        customerPage.manageCustomersUserBtn(customerPage.getCustomerName()).click();

        Assert.assertTrue(urlContains("user"));
        Assert.assertNotNull(customerPage.customerUserIconHeader());
        Assert.assertTrue(customerPage.customerUserIconHeader().isDisplayed());
        Assert.assertTrue(customerPage.customerManageWindowIconHead().getText().contains(iconText));
    }

    @Test(groups = "smoke")
    @Description
    public void openWindowByView() {
        sideBarMenuView.customerBtn().click();
        customerPage.setCustomerName();
        customerPage.entity(customerPage.getCustomerName()).click();
        customerPage.manageCustomersUserBtnView().click();

        Assert.assertTrue(urlContains("user"));
        Assert.assertNotNull(customerPage.customerUserIconHeader());
        Assert.assertTrue(customerPage.customerUserIconHeader().isDisplayed());
        Assert.assertTrue(customerPage.customerManageWindowIconHead().getText().contains(iconText));
    }
}
