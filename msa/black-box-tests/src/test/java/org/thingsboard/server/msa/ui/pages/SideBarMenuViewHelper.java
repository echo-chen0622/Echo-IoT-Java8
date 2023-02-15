package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;

public class SideBarMenuViewHelper extends SideBarMenuViewElements {
    public SideBarMenuViewHelper(WebDriver driver) {
        super(driver);
    }

    public void openDeviceProfiles() {
        profilesBtn().click();
        deviceProfileBtn().click();
    }

    public void openAssetProfiles() {
        profilesBtn().click();
        assetProfileBtn().click();
    }
}
