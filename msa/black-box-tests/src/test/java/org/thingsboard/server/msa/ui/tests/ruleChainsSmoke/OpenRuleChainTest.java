package org.thingsboard.server.msa.ui.tests.ruleChainsSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.OpenRuleChainPageHelper;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

public class OpenRuleChainTest extends AbstractDriverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private RuleChainsPageHelper ruleChainsPage;
    private OpenRuleChainPageHelper openRuleChainPage;
    private String ruleChainName;

    @BeforeMethod
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        ruleChainsPage = new RuleChainsPageHelper(driver);
        openRuleChainPage = new OpenRuleChainPageHelper(driver);
    }

    @AfterMethod
    public void delete() {
        if (ruleChainName != null) {
            testRestClient.deleteRuleChain(getRuleChainByName(ruleChainName).getId());
            ruleChainName = null;
        }
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void openRuleChainByRightCornerBtn() {
        String ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(EntityPrototypes.defaultRuleChainPrototype(ruleChainName));
        this.ruleChainName = ruleChainName;

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.entity(ruleChainName).click();
        openRuleChainPage.setHeadName();

        Assert.assertTrue(urlContains(String.valueOf(getRuleChainByName(ruleChainName).getId())));
        Assert.assertTrue(openRuleChainPage.headRuleChainName().isDisplayed());
        Assert.assertTrue(openRuleChainPage.inputNode().isDisplayed());
        Assert.assertEquals(ruleChainName, openRuleChainPage.getHeadName());
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void openRuleChainByViewBtn() {
        String ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(EntityPrototypes.defaultRuleChainPrototype(ruleChainName));
        this.ruleChainName = ruleChainName;

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ruleChainName).click();
        ruleChainsPage.openRuleChainFromViewBtn().click();
        openRuleChainPage.setHeadName();

        Assert.assertTrue(urlContains(String.valueOf(getRuleChainByName(ruleChainName).getId())));
        Assert.assertTrue(openRuleChainPage.headRuleChainName().isDisplayed());
        Assert.assertTrue(openRuleChainPage.inputNode().isDisplayed());
        Assert.assertEquals(ruleChainName, openRuleChainPage.getHeadName());
    }
}
