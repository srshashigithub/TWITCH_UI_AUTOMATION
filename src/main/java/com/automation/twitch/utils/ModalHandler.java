package com.automation.twitch.utils;

import com.automation.twitch.config.ConfigManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ModalHandler {

    private static final Logger log = LoggerFactory.getLogger(ModalHandler.class);

    private static final long MODAL_TIMEOUT      = ConfigManager.getLong("modal.check.timeout");
    private static final long QUICK_CHECK_SECS   = 1; // fast presence probe before full clickable wait

    // "Open in App / Continue in Browser" prompt — checked before any page interaction
    private static final List<By> OPEN_IN_BROWSER_SELECTORS = List.of(
            By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'continue in browser')]"),
            By.xpath("//a[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'continue in browser')]"),
            By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'use browser')]"),
            By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'use the browser')]"),
            By.cssSelector("[data-a-target='open-in-browser-button']"),
            By.cssSelector("[data-a-target='skip-to-browser']")
    );

    // All known Twitch modal/popup dismiss patterns, ordered by likelihood
    private static final List<By> DISMISS_SELECTORS = List.of(
            By.cssSelector("[data-a-target='player-overlay-mature-accept']"),
            By.cssSelector("[data-a-target='consent-banner-accept']"),
            By.cssSelector("[data-a-target='modal-close-button']"),
            By.cssSelector("[data-a-target='prime-offer-close-button']"),
            By.cssSelector("[data-a-target='age-gate-confirm']"),
            By.cssSelector("[data-a-target='bits-buy-button-close']"),
            By.cssSelector("div[role='dialog'] button[aria-label='Close']"),
            By.cssSelector("div[role='dialog'] button[aria-label='close']")
    );

    private final WebDriver driver;

    public ModalHandler(WebDriver driver) {
        this.driver = Objects.requireNonNull(driver, "WebDriver must not be null");
    }

    /**
     * Dismisses the "Open in App / Continue in Browser" prompt Twitch shows on first load.
     * Tries explicit buttons first; if none found, falls back to clicking a safe blank area.
     */
    public void dismissAppPromptIfPresent() {
        log.info("Checking for 'Open in App / Use Browser' prompt...");

        for (By selector : OPEN_IN_BROWSER_SELECTORS) {
            Optional<WebElement> button = findIfClickable(selector);
            if (button.isPresent()) {
                log.info("App redirect prompt detected — clicking 'Continue in Browser' [{}]", selector);
                try {
                    button.get().click();
                    waitForInvisibility(selector); // replaces hard sleep
                    log.info("App prompt dismissed via button click");
                    return;
                } catch (Exception e) {
                    log.warn("Button click failed, trying next: {}", e.getMessage());
                }
            }
        }

        // Fallback: click top-left body corner (far from any centred popup)
        log.info("No explicit dismiss button found — clicking blank area as fallback");
        try {
            WebElement body = driver.findElement(By.tagName("body"));
            int halfWidth = (int) (((Long) ((JavascriptExecutor) driver)
                    .executeScript("return document.documentElement.clientWidth")) / 2);
            new Actions(driver)
                    .moveToElement(body, -(halfWidth - 10), -300)
                    .click()
                    .perform();
            log.info("Blank-area click performed");
        } catch (Exception e) {
            log.warn("Blank-area fallback click failed: {}", e.getMessage());
        }
    }

    public void dismissIfPresent() {
        log.info("Scanning for modals/pop-ups...");
        boolean dismissed = false;

        for (By selector : DISMISS_SELECTORS) {
            // 1-second quick probe avoids paying the full MODAL_TIMEOUT for absent selectors
            if (!isQuicklyPresent(selector, QUICK_CHECK_SECS)) continue;

            Optional<WebElement> button = findIfClickable(selector);
            if (button.isPresent()) {
                log.info("Modal detected [{}] — dismissing", selector);
                try {
                    button.get().click();
                    dismissed = true;
                    waitForInvisibility(selector); // replaces hard sleep
                    log.info("Modal dismissed");
                } catch (Exception e) {
                    log.warn("Modal dismiss failed: {}", e.getMessage());
                }
            }
        }

        if (!dismissed) log.info("No modals detected");
    }

    public void dismissKeepUsingWebIfPresent() {
        By locator = By.xpath("//p[text()='Keep using web']");
        Optional<WebElement> element = findIfClickable(locator);
        if (element.isPresent()) {
            log.info("'Keep using web' prompt detected — clicking");
            try {
                element.get().click();
                waitForInvisibility(locator); // replaces hard sleep
                log.info("'Keep using web' dismissed");
            } catch (Exception e) {
                log.warn("Click on 'Keep using web' failed: {}", e.getMessage());
            }
        } else {
            log.info("'Keep using web' prompt not present");
        }
    }

    public boolean isAppPromptPresent() {
        return OPEN_IN_BROWSER_SELECTORS.stream().anyMatch(sel -> isQuicklyPresent(sel, 2));
    }

    public boolean isAnyModalPresent() {
        return DISMISS_SELECTORS.stream().anyMatch(sel -> isQuicklyPresent(sel, 2));
    }

    private void waitForInvisibility(By locator) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(MODAL_TIMEOUT))
                    .until(ExpectedConditions.invisibilityOfElementLocated(locator));
        } catch (TimeoutException e) {
            log.debug("Element [{}] still visible after dismiss — continuing", locator);
        }
    }

    private boolean isQuicklyPresent(By locator, long timeoutSeconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                    .until(ExpectedConditions.presenceOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    private Optional<WebElement> findIfClickable(By locator) {
        try {
            return Optional.of(new WebDriverWait(driver, Duration.ofSeconds(MODAL_TIMEOUT))
                    .until(ExpectedConditions.elementToBeClickable(locator)));
        } catch (TimeoutException e) {
            return Optional.empty();
        }
    }
}
