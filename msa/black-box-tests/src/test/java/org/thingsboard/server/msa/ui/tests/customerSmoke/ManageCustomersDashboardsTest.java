package org.thingsboard.server.msa.ui.tests.customerSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

public class ManageCustomersDashboardsTest extends AbstractDriverBaseTest {
    private SideBarMenuViewElements sideBarMenuView;
    private CustomerPageHelper customerPage;
    private final String manage = "Dashboards";

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
        customerPage.manageCustomersDashboardsBtn(customerPage.getCustomerName()).click();

        Assert.assertTrue(urlContains(manage.toLowerCase()));
        Assert.assertNotNull(customerPage.customerDashboardIconHeader());
        Assert.assertTrue(customerPage.customerDashboardIconHeader().isDisplayed());
        Assert.assertTrue(customerPage.customerManageWindowIconHead().getText().contains(manage));
    }

    @Test(groups = "smoke")
    @Description
    public void openWindowByView() {
        sideBarMenuView.customerBtn().click();
        customerPage.setCustomerName();
        customerPage.entity(customerPage.getCustomerName()).click();
        customerPage.manageCustomersDashboardsBtnView().click();

        Assert.assertTrue(urlContains(manage.toLowerCase()));
        Assert.assertNotNull(customerPage.customerDashboardIconHeader());
        Assert.assertTrue(customerPage.customerDashboardIconHeader().isDisplayed());
        Assert.assertTrue(customerPage.customerManageWindowIconHead().getText().contains(manage));
    }
}
