package com.automation.twitch.tests;

import com.automation.twitch.driver.DriverFactory;
import com.automation.twitch.pages.HomePage;
import com.automation.twitch.pages.SearchPage;
import com.automation.twitch.pages.StreamerPage;
import com.automation.twitch.utils.ModalHandler;
import com.automation.twitch.utils.ScreenshotUtil;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TwitchSearchTest {

    private static final Logger log = LoggerFactory.getLogger(TwitchSearchTest.class);

    private WebDriver driver;
    private ModalHandler modalHandler;

    @BeforeMethod
    public void setUp() {
        driver = DriverFactory.getDriver();
        modalHandler = new ModalHandler(driver);
        log.info("──── Test setup complete ────");
    }

    @Test(description = "Search StarCraft II on Twitch (mobile), scroll, open a streamer page, take screenshot")
    public void should_capture_streamer_page_when_searching_starcraftII_on_mobile() {
        log.info("STEP 1 — Open Twitch home page");
        HomePage homePage = new HomePage(driver).open();

        // Click "Keep using web" if the app-redirect prompt appears
        modalHandler.dismissKeepUsingWebIfPresent();

        log.info("STEP 2 — Click search icon");
        SearchPage searchPage = homePage.clickSearchIcon();

        log.info("STEP 3 — Type 'StarCraft II'");
        searchPage.typeQuery("StarCraft II")
                  .submitSearch();

        log.info("STEP 4 — Scroll down 2 times");
        searchPage.scrollResults(2);

        log.info("STEP 4a — Screenshot of streamer list page");
        String listScreenshot = searchPage.captureListPage("starcraftii_streamer_list");

        assertThat(listScreenshot)
                .as("Streamer list screenshot should be created")
                .isNotNull()
                .endsWith(".png");

        log.info("STEP 5 — Select first available streamer");
        StreamerPage streamerPage = searchPage.selectFirstStreamer();

        // Handle mature content warning, age gate, or any overlay before page settles
        modalHandler.dismissIfPresent();

        log.info("STEP 6 — Wait for full page load");
        streamerPage.waitForPageReady();

        String screenshotPath = ScreenshotUtil.capture(driver, "starcraftii_streamer");

        assertThat(screenshotPath)
                .as("Screenshot file should be created")
                .isNotNull()
                .endsWith(".png");

        assertThat(streamerPage.getCurrentUrl())
                .as("Should be on a Twitch channel page")
                .contains("twitch.tv")
                .doesNotContain("/search");

        log.info("✓ Test passed. Screenshot: {}", screenshotPath);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        log.info("──── Tearing down driver ────");
        DriverFactory.quitDriver();
    }
}
