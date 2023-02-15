package org.thingsboard.server.msa.ui.tests.customerSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;

import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultCustomerPrototype;

public class SortByNameTest extends AbstractDriverBaseTest {
    private SideBarMenuViewElements sideBarMenuView;
    private CustomerPageHelper customerPage;
    private String customerName;

    @BeforeMethod
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        customerPage = new CustomerPageHelper(driver);
    }

    @AfterMethod
    public void delete() {
        if (customerName != null) {
            testRestClient.deleteCustomer(getCustomerByName(customerName).getId());
            customerName = null;
        }
    }

    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description
    public void specialCharacterUp(String title) {
        testRestClient.postCustomer(defaultCustomerPrototype(title));
        this.customerName = title;

        sideBarMenuView.customerBtn().click();
        customerPage.sortByTitleBtn().click();
        customerPage.setCustomerName();

        Assert.assertEquals(customerPage.getCustomerName(), title);
    }

    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description
    public void allSortUp(String customer, String customerSymbol, String customerNumber) {
        testRestClient.postCustomer(defaultCustomerPrototype(customerSymbol));
        testRestClient.postCustomer(defaultCustomerPrototype(customer));
        testRestClient.postCustomer(defaultCustomerPrototype(customerNumber));

        sideBarMenuView.customerBtn().click();
        customerPage.sortByTitleBtn().click();
        customerPage.setCustomerName(0);
        String firstCustomer = customerPage.getCustomerName();
        customerPage.setCustomerName(1);
        String secondCustomer = customerPage.getCustomerName();
        customerPage.setCustomerName(2);
        String thirdCustomer = customerPage.getCustomerName();

        testRestClient.deleteCustomer(getCustomerByName(customer).getId());
        testRestClient.deleteCustomer(getCustomerByName(customerNumber).getId());
        testRestClient.deleteCustomer(getCustomerByName(customerSymbol).getId());

        Assert.assertEquals(firstCustomer, customerSymbol);
        Assert.assertEquals(secondCustomer, customerNumber);
        Assert.assertEquals(thirdCustomer, customer);
    }

    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description
    public void specialCharacterDown(String title) {
        testRestClient.postCustomer(defaultCustomerPrototype(title));
        customerName = title;

        sideBarMenuView.customerBtn().click();
        customerPage.sortByNameDown();
        customerPage.setCustomerName(customerPage.allEntity().size() - 1);

        Assert.assertEquals(customerPage.getCustomerName(), title);
    }

    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description
    public void allSortDown(String customer, String customerSymbol, String customerNumber) {
        testRestClient.postCustomer(defaultCustomerPrototype(customerSymbol));
        testRestClient.postCustomer(defaultCustomerPrototype(customer));
        testRestClient.postCustomer(defaultCustomerPrototype(customerNumber));

        sideBarMenuView.customerBtn().click();
        int lastIndex = customerPage.allEntity().size() - 1;
        customerPage.sortByNameDown();
        customerPage.setCustomerName(lastIndex);
        String firstCustomer = customerPage.getCustomerName();
        customerPage.setCustomerName(lastIndex - 1);
        String secondCustomer = customerPage.getCustomerName();
        customerPage.setCustomerName(lastIndex - 2);
        String thirdCustomer = customerPage.getCustomerName();

        testRestClient.deleteCustomer(getCustomerByName(customer).getId());
        testRestClient.deleteCustomer(getCustomerByName(customerNumber).getId());
        testRestClient.deleteCustomer(getCustomerByName(customerSymbol).getId());

        Assert.assertEquals(firstCustomer, customerSymbol);
        Assert.assertEquals(secondCustomer, customerNumber);
        Assert.assertEquals(thirdCustomer, customer);
    }
}
