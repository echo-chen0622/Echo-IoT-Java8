package org.thingsboard.server.msa;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.internal.ConstructorOrMethod;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;

import static org.testng.internal.Utils.log;
import static org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest.captureScreen;

@Slf4j
public class TestListener implements ITestListener {

    WebDriver driver;

    @Override
    public void onTestStart(ITestResult result) {
        log.info("===>>> Test started: " + result.getName());
    }

    /**
     * Invoked when a test succeeds
     */
    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("<<<=== Test completed successfully: " + result.getName());
        ConstructorOrMethod consOrMethod = result.getMethod().getConstructorOrMethod();
        DisableUIListeners disable = consOrMethod.getMethod().getDeclaringClass().getAnnotation(DisableUIListeners.class);
        if (disable != null) {
            return;
        }
        driver = ((AbstractDriverBaseTest) result.getInstance()).getDriver();
        captureScreen(driver, "success");
    }

    /**
     * Invoked when a test fails
     */
    @Override
    public void onTestFailure(ITestResult result) {
        log.info("<<<=== Test failed: " + result.getName());
        ConstructorOrMethod consOrMethod = result.getMethod().getConstructorOrMethod();
        DisableUIListeners disable = consOrMethod.getMethod().getDeclaringClass().getAnnotation(DisableUIListeners.class);
        if (disable != null) {
            return;
        }
        driver = ((AbstractDriverBaseTest) result.getInstance()).getDriver();
        captureScreen(driver, "failure");
    }

    /**
     * Invoked when a test skipped
     */
    @Override
    public void onTestSkipped(ITestResult result) {
        log.info("<<<=== Test skipped: " + result.getName());
        ConstructorOrMethod consOrMethod = result.getMethod().getConstructorOrMethod();
        DisableUIListeners disable = consOrMethod.getMethod().getDeclaringClass().getAnnotation(DisableUIListeners.class);
        if (disable != null) {
            return;
        }
        driver = ((AbstractDriverBaseTest) result.getInstance()).getDriver();
        captureScreen(driver, "skipped");
    }
}
