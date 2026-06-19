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
import java.util.Optional;

public class ModalHandler {

    private static final Logger log = LoggerFactory.getLogger(ModalHandler.class);

    private static final long MODAL_TIMEOUT = ConfigManager.getLong("modal.check.timeout");

    // "Open in App / Continue in Browser" prompt — checked before any page interaction
    private static final List<By> OPEN_IN_BROWSER_SELECTORS = List.of(
            By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'continue in browser')]"),
            By.xpath("//a[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'continue in browser')]"),
            By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'use browser')]"),
            By.xpath("//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'use the browser')]"),
            By.cssSelector("[data-a-target='open-in-browser-button']"),
            By.cssSelector("[data-a-target='skip-to-browser']")
    );

    // All known Twitch modal/popup dismiss patterns, checked in order
    private static final List<By> DISMISS_SELECTORS = List.of(
            // Mature content warning — "Start Watching" or "I understand, let me in"
            By.cssSelector("[data-a-target='player-overlay-mature-accept']"),
            // Cookie / GDPR consent banner
            By.cssSelector("[data-a-target='consent-banner-accept']"),
            // Generic modal close button
            By.cssSelector("[data-a-target='modal-close-button']"),
            // Prime / subscription offer close
            By.cssSelector("[data-a-target='prime-offer-close-button']"),
            // Age gate confirm
            By.cssSelector("[data-a-target='age-gate-confirm']"),
            // Bits / channel points tooltip dismiss
            By.cssSelector("[data-a-target='bits-buy-button-close']"),
            // Generic close icon inside role=dialog
            By.cssSelector("div[role='dialog'] button[aria-label='Close']"),
            By.cssSelector("div[role='dialog'] button[aria-label='close']")
    );

    private final WebDriver driver;

    public ModalHandler(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Dismisses the "Open in App / Continue in Browser" prompt that Twitch shows
     * on first load. Tries explicit "Continue in Browser" buttons first; if none
     * are found but the overlay backdrop is still covering the page, falls back to
     * clicking a safe blank area (top-left corner of <body>) which is what a real
     * user would do to dismiss it.
     */
    public void dismissAppPromptIfPresent() {
        log.info("Checking for 'Open in App / Use Browser' prompt...");

        for (By selector : OPEN_IN_BROWSER_SELECTORS) {
            Optional<WebElement> button = findIfClickable(selector);
            if (button.isPresent()) {
                log.info("App redirect prompt detected — clicking 'Continue in Browser' [{}]", selector);
                try {
                    button.get().click();
                    sleepMillis(800);
                    log.info("App prompt dismissed via button click");
                    return;
                } catch (Exception e) {
                    log.warn("Button click failed, trying next: {}", e.getMessage());
                }
            }
        }

        // Fallback: click the top-left corner of <body> — equivalent to clicking blank UI
        log.info("No explicit dismiss button found — clicking blank area as fallback");
        try {
            WebElement body = driver.findElement(By.tagName("body"));
            int halfWidth  = (int) (((Long) ((JavascriptExecutor) driver)
                    .executeScript("return document.documentElement.clientWidth")) / 2);
            // Click at (10, 10) relative to the body top-left, far from any centred popup
            new Actions(driver)
                    .moveToElement(body, -(halfWidth - 10), -300)
                    .click()
                    .perform();
            sleepMillis(600);
            log.info("Blank-area click performed");
        } catch (Exception e) {
            log.warn("Blank-area fallback click failed: {}", e.getMessage());
        }
    }

    public void dismissIfPresent() {
        log.info("Scanning for modals/pop-ups...");
        boolean dismissed = false;

        for (By selector : DISMISS_SELECTORS) {
            Optional<WebElement> button = findIfClickable(selector);
            if (button.isPresent()) {
                log.info("Modal detected [{}] — dismissing", selector);
                try {
                    button.get().click();
                    dismissed = true;
                    sleepMillis(600);
                    log.info("Modal dismissed successfully");
                } catch (Exception e) {
                    log.warn("Click on modal button failed, continuing: {}", e.getMessage());
                }
            }
        }

        if (!dismissed) {
            log.info("No modals detected");
        }
    }

    public void dismissKeepUsingWebIfPresent() {
        By locator = By.xpath("//p[text()='Keep using web']");
        Optional<WebElement> element = findIfClickable(locator);
        if (element.isPresent()) {
            log.info("'Keep using web' prompt detected — clicking");
            try {
                element.get().click();
                sleepMillis(600);
                log.info("'Keep using web' dismissed");
            } catch (Exception e) {
                log.warn("Click on 'Keep using web' failed: {}", e.getMessage());
            }
        } else {
            log.info("'Keep using web' prompt not present");
        }
    }

    /** Quick presence check (2 s) — use as a guard before the full dismiss call. */
    public boolean isAppPromptPresent() {
        return OPEN_IN_BROWSER_SELECTORS.stream()
                .anyMatch(sel -> isQuicklyPresent(sel, 2));
    }

    /** Quick presence check (2 s) — use as a guard before the full dismiss call. */
    public boolean isAnyModalPresent() {
        return DISMISS_SELECTORS.stream()
                .anyMatch(sel -> isQuicklyPresent(sel, 2));
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
            WebElement element = new WebDriverWait(driver, Duration.ofSeconds(MODAL_TIMEOUT))
                    .until(ExpectedConditions.elementToBeClickable(locator));
            return Optional.of(element);
        } catch (TimeoutException e) {
            return Optional.empty();
        }
    }

    private void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
