package org.thingsboard.server.msa.ui.tests.ruleChainsSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomNumber;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_RULE_CHAIN_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

public class RuleChainEditMenuTest extends AbstractDriverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private RuleChainsPageHelper ruleChainsPage;
    private String ruleChainName;

    @BeforeMethod
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        ruleChainsPage = new RuleChainsPageHelper(driver);
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
    public void changeName() {
        String newRuleChainName = "Changed" + getRandomNumber();
        String ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));
        this.ruleChainName = ruleChainName;

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ruleChainName).click();
        ruleChainsPage.setHeaderName();
        String nameBefore = ruleChainsPage.getHeaderName();
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.changeNameEditMenu(newRuleChainName);
        ruleChainsPage.doneBtnEditView().click();
        this.ruleChainName = newRuleChainName;
        ruleChainsPage.setHeaderName();
        String nameAfter = ruleChainsPage.getHeaderName();

        Assert.assertNotEquals(nameBefore, nameAfter);
        Assert.assertEquals(newRuleChainName, nameAfter);
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void deleteName() {
        String ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));
        this.ruleChainName = ruleChainName;

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ruleChainName).click();
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.changeNameEditMenu("");

        Assert.assertFalse(ruleChainsPage.doneBtnEditViewVisible().isEnabled());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void saveOnlyWithSpace() {
        String ruleChainName = ENTITY_NAME +random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));
        this.ruleChainName = ruleChainName;

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ruleChainName).click();
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.changeNameEditMenu(" ");
        ruleChainsPage.doneBtnEditView().click();

        Assert.assertNotNull(ruleChainsPage.warningMessage());
        Assert.assertTrue(ruleChainsPage.warningMessage().isDisplayed());
        Assert.assertEquals(ruleChainsPage.warningMessage().getText(), EMPTY_RULE_CHAIN_MESSAGE);
    }

    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "editMenuDescription")
    @Description
    public void editDescription(String description, String newDescription, String finalDescription) {
        String name = ENTITY_NAME + random();
        testRestClient.postRuleChain(EntityPrototypes.defaultRuleChainPrototype(name, description));
        ruleChainName = name;

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(name).click();
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.descriptionEntityView().sendKeys(newDescription);
        ruleChainsPage.doneBtnEditView().click();
        ruleChainsPage.setDescription();

        Assert.assertEquals(ruleChainsPage.getDescription(), finalDescription);
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void debugMode() {
        String ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));
        this.ruleChainName = ruleChainName;

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ruleChainName).click();
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.debugCheckboxEdit().click();
        ruleChainsPage.doneBtnEditView().click();
        boolean debugMode = Boolean.parseBoolean(ruleChainsPage.debugCheckboxView().getAttribute("aria-checked"));
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.debugCheckboxEdit().click();
        ruleChainsPage.doneBtnEditView().click();

        Assert.assertFalse(Boolean.parseBoolean(ruleChainsPage.debugCheckboxView().getAttribute("aria-checked")));
        Assert.assertTrue(debugMode);
    }
}
