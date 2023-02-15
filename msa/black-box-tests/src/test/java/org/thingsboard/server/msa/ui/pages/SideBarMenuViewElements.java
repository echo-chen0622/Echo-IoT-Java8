package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.thingsboard.server.msa.ui.base.AbstractBasePage;

public class SideBarMenuViewElements extends AbstractBasePage {
    public SideBarMenuViewElements(WebDriver driver) {
        super(driver);
    }

    private static final String RULE_CHAINS_BTN = "//mat-toolbar//a[@href='/ruleChains']";
    private static final String CUSTOMER_BTN = "//mat-toolbar//a[@href='/customers']";
    private static final String DASHBOARD_BTN = "//mat-toolbar//a[@href='/dashboards']";
    private static final String PROFILES_BTN = "//mat-toolbar//a[@href='/profiles']";
    private static final String DEVICE_PROFILE_BTN = "//mat-toolbar//a[@href='/profiles/deviceProfiles']";
    private static final String ASSET_PROFILE_BTN = "//mat-toolbar//a[@href='/profiles/assetProfiles']";

    public WebElement ruleChainsBtn() {
        return waitUntilElementToBeClickable(RULE_CHAINS_BTN);
    }

    public WebElement customerBtn() {
        return waitUntilElementToBeClickable(CUSTOMER_BTN);
    }

    public WebElement dashboardBtn() {
        return waitUntilElementToBeClickable(DASHBOARD_BTN);
    }

    public WebElement profilesBtn() {
        return waitUntilElementToBeClickable(PROFILES_BTN);
    }

    public WebElement deviceProfileBtn() {
        return waitUntilElementToBeClickable(DEVICE_PROFILE_BTN);
    }

    public WebElement assetProfileBtn() {
        return waitUntilElementToBeClickable(ASSET_PROFILE_BTN);
    }
}
