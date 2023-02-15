package org.thingsboard.server.msa.ui.tests.ruleChainsSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.DELETE_RULE_CHAIN_WITH_PROFILE_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.ROOT_RULE_CHAIN_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

public class DeleteRuleChainTest extends AbstractDriverBaseTest {
    private SideBarMenuViewElements sideBarMenuView;
    private RuleChainsPageHelper ruleChainsPage;

    @BeforeMethod
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        ruleChainsPage = new RuleChainsPageHelper(driver);
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void removeRuleChainByRightSideBtn() {
        String ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        String deletedRuleChain = ruleChainsPage.deleteRuleChainTrash(ruleChainName);
        ruleChainsPage.refreshBtn().click();

        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(deletedRuleChain));
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void removeSelectedRuleChain() {
        String ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        String deletedRuleChain = ruleChainsPage.deleteSelected(ruleChainName);
        ruleChainsPage.refreshBtn().click();

        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(deletedRuleChain));
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void removeFromRuleChainView() {
        String ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ENTITY_NAME).click();
        String deletedRuleChain = ruleChainsPage.deleteRuleChainFromView(ruleChainName);
        ruleChainsPage.refreshBtn().click();

        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(deletedRuleChain));
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void removeRootRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertFalse(ruleChainsPage.deleteBtn(ROOT_RULE_CHAIN_NAME).isEnabled());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void removeSelectedRootRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();

        ruleChainsPage.assertCheckBoxIsNotDisplayed(ROOT_RULE_CHAIN_NAME);
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void removeFromRootRuleChainView() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ROOT_RULE_CHAIN_NAME).click();
        ruleChainsPage.deleteBtnFromView();

        Assert.assertTrue(ruleChainsPage.deleteBtnInRootRuleChainIsNotDisplayed());
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void removeProfileRuleChainByRightSideBtn() {
        String deletedRuleChain = "Thermostat";

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.deleteBtn(deletedRuleChain).click();
        ruleChainsPage.warningPopUpYesBtn().click();
        ruleChainsPage.refreshBtn().click();

        Assert.assertNotNull(ruleChainsPage.entity(deletedRuleChain));
        Assert.assertTrue(ruleChainsPage.entity(deletedRuleChain).isDisplayed());
        Assert.assertNotNull(ruleChainsPage.warningMessage());
        Assert.assertTrue(ruleChainsPage.warningMessage().isDisplayed());
        Assert.assertEquals(ruleChainsPage.warningMessage().getText(), DELETE_RULE_CHAIN_WITH_PROFILE_MESSAGE);
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void removeSelectedProfileRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();
        String deletedRuleChain = ruleChainsPage.deleteSelected("Thermostat");
        ruleChainsPage.refreshBtn().click();

        Assert.assertNotNull(ruleChainsPage.entity(deletedRuleChain));
        Assert.assertTrue(ruleChainsPage.entity(deletedRuleChain).isDisplayed());
        Assert.assertNotNull(ruleChainsPage.warningMessage());
        Assert.assertTrue(ruleChainsPage.warningMessage().isDisplayed());
        Assert.assertEquals(ruleChainsPage.warningMessage().getText(), DELETE_RULE_CHAIN_WITH_PROFILE_MESSAGE);
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void removeFromProfileRuleChainView() {
        String deletedRuleChain = "Thermostat";

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(deletedRuleChain).click();
        ruleChainsPage.deleteBtnFromView().click();
        ruleChainsPage.warningPopUpYesBtn().click();

        Assert.assertNotNull(ruleChainsPage.entity(deletedRuleChain));
        Assert.assertNotNull(ruleChainsPage.warningMessage());
        Assert.assertTrue(ruleChainsPage.warningMessage().isDisplayed());
        Assert.assertEquals(ruleChainsPage.warningMessage().getText(), DELETE_RULE_CHAIN_WITH_PROFILE_MESSAGE);
    }

    @Test(priority = 30, groups = "smoke")
    @Description
    public void removeRuleChainByRightSideBtnWithoutRefresh() {
        String ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        String deletedRuleChain = ruleChainsPage.deleteRuleChainTrash(ruleChainName);

        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(deletedRuleChain));
    }
}
