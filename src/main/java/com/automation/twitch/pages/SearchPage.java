package com.automation.twitch.pages;

import com.automation.twitch.utils.ScreenshotUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchPage extends BasePage {

    private static final Logger log = LoggerFactory.getLogger(SearchPage.class);

    // Twitch uses data-a-target attributes as stable identifiers for their own testing
    private static final By SEARCH_INPUT = By.cssSelector(
            "input[data-a-target='tw-input'], input[type='search'], input[placeholder*='Search']"
    );

    // First anchor on the page whose href is a plain channel path (e.g. /ninja),
    // excluding directories, search, and static pages
    private static final By FIRST_STREAMER_LINK = By.xpath(
            "(//a[starts-with(@href,'/') and string-length(@href) > 1" +
            " and not(contains(@href,'/directory'))" +
            " and not(contains(@href,'/search'))" +
            " and not(contains(@href,'/p/'))])[1]"
    );

    public SearchPage(WebDriver driver) {
        super(driver);
    }

    public SearchPage typeQuery(String query) {
        log.info("Typing search query: '{}'", query);
        WebElement input = waitForVisible(SEARCH_INPUT);
        input.clear();
        input.sendKeys(query);
        return this;
    }

    public SearchPage submitSearch() {
        log.info("Submitting search (Enter key)");
        waitForVisible(SEARCH_INPUT).sendKeys(Keys.ENTER);
        waitForDocumentReady();
        return this;
    }

    public SearchPage scrollResults(int times) {
        log.info("Scrolling down {} time(s) on results page", times);
        scrollDown(times);
        return this;
    }

    public String captureListPage(String label) {
        log.info("Waiting for streamer list to be visible before screenshot...");
        waitForClickable(FIRST_STREAMER_LINK);
        String path = ScreenshotUtil.capture(driver, label);
        log.info("Streamer list screenshot saved → {}", path);
        return path;
    }

    public StreamerPage selectFirstStreamer() {
        log.info("Waiting for a streamer link to be clickable...");
        WebElement streamer = waitForClickable(FIRST_STREAMER_LINK);
        if (log.isInfoEnabled()) {
            log.info("Clicking streamer: {}", streamer.getAttribute("href"));
        }
        streamer.click();
        return new StreamerPage(driver);
    }
}
