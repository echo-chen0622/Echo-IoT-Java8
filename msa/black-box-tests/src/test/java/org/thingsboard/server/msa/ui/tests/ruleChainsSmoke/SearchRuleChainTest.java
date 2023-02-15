package org.thingsboard.server.msa.ui.tests.ruleChainsSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;

import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

public class SearchRuleChainTest extends AbstractDriverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private RuleChainsPageHelper ruleChainsPage;

    @BeforeMethod
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        ruleChainsPage = new RuleChainsPageHelper(driver);
    }

    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "ruleChainNameForSearchByFirstAndSecondWord")
    @Description
    public void searchFirstWord(String namePath) {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.searchEntity(namePath);
        ruleChainsPage.setRuleChainName(0);

        Assert.assertTrue(ruleChainsPage.getRuleChainName().contains(namePath));
    }

    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSearchBySymbolAndNumber")
    @Description
    public void searchNumber(String name, String namePath) {
        testRestClient.postRuleChain(defaultRuleChainPrototype(name));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.searchEntity(namePath);
        ruleChainsPage.setRuleChainName(0);
        boolean ruleChainContainsNamePath = ruleChainsPage.getRuleChainName().contains(namePath);

        testRestClient.deleteRuleChain(getRuleChainByName(name).getId());

        Assert.assertTrue(ruleChainContainsNamePath);
    }
}
