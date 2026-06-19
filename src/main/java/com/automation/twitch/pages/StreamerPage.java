package com.automation.twitch.pages;

import com.automation.twitch.config.ConfigManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamerPage extends BasePage {

    private static final Logger log = LoggerFactory.getLogger(StreamerPage.class);

    private static final long STABILIZATION_MS = ConfigManager.getLong("streamer.stabilization.ms");

    private static final By VIDEO_PLAYER    = By.cssSelector(".video-player, [data-a-target='video-player'], .persistent-player");
    private static final By LOADING_SPINNER = By.cssSelector(".tw-loading-spinner, [data-a-target='loading-spinner']");

    public StreamerPage(WebDriver driver) {
        super(driver);
    }

    public StreamerPage waitForPageReady() {
        log.info("Waiting for streamer page to fully load...");

        // 1. DOM must be complete
        waitForDocumentReady();

        // 2. Video player container must be present
        wait.until(ExpectedConditions.presenceOfElementLocated(VIDEO_PLAYER));
        log.info("Video player container detected");

        // 3. Any loading spinners must disappear
        wait.until(ExpectedConditions.invisibilityOfElementLocated(LOADING_SPINNER));

        // 4. Configurable stabilisation pause — React components may still be mounting
        sleepMillis(STABILIZATION_MS);

        log.info("Streamer page ready. Current URL: {}", driver.getCurrentUrl());
        return this;
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
