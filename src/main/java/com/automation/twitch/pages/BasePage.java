package com.automation.twitch.pages;

import com.automation.twitch.config.ConfigManager;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    private static final Logger log = LoggerFactory.getLogger(BasePage.class);
    private static final long EXPLICIT_WAIT_SECONDS = ConfigManager.getLong("explicit.wait");
    private static final long POLL_INTERVAL_MS = 500;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(EXPLICIT_WAIT_SECONDS));
    }

    protected WebElement waitForVisible(By locator) {
        log.debug("Waiting for visible: {}", locator);
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitForClickable(By locator) {
        log.debug("Waiting for clickable: {}", locator);
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    // FluentWait retries on StaleElement and InterceptedClick — critical for Twitch's React DOM
    protected void click(By locator) {
        log.debug("Clicking: {}", locator);
        new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(EXPLICIT_WAIT_SECONDS))
                .pollingEvery(Duration.ofMillis(POLL_INTERVAL_MS))
                .ignoring(StaleElementReferenceException.class)
                .ignoring(ElementClickInterceptedException.class)
                .until(d -> {
                    d.findElement(locator).click();
                    return true;
                });
    }

    protected void type(By locator, String text) {
        log.debug("Typing '{}' into: {}", text, locator);
        WebElement element = waitForVisible(locator);
        element.clear();
        element.sendKeys(text);
    }

    protected void scrollDown() {
        ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, window.innerHeight)");
        sleepMillis(800);
    }

    protected void scrollDown(int times) {
        for (int i = 1; i <= times; i++) {
            log.info("Scrolling down [{}/{}]", i, times);
            scrollDown();
        }
    }

    protected boolean isPresent(By locator, long timeoutSeconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                    .until(ExpectedConditions.presenceOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    protected List<WebElement> findAll(By locator) {
        return driver.findElements(locator);
    }

    protected void waitForDocumentReady() {
        wait.until(d -> "complete".equals(
                ((JavascriptExecutor) d).executeScript("return document.readyState")));
    }

    protected void jsClick(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    protected void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
