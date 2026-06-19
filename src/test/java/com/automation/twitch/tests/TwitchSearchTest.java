package com.automation.twitch.tests;

import com.automation.twitch.driver.DriverFactory;
import com.automation.twitch.pages.HomePage;
import com.automation.twitch.pages.SearchPage;
import com.automation.twitch.pages.StreamerPage;
import com.automation.twitch.utils.ModalHandler;
import com.automation.twitch.utils.RetryAnalyzer;
import com.automation.twitch.utils.ScreenshotUtil;
import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
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

    // ── Data provider ──────────────────────────────────────────────────────────

    @DataProvider(name = "searchQueries", parallel = true)
    public static Object[][] searchQueries() {
        return new Object[][]{
                {"Valorant"},
                {"League of Legends"},
        };
    }

    // ── Test 1: full E2E flow ──────────────────────────────────────────────────

    @Test(
        description   = "Search StarCraft II on Twitch (mobile), scroll, open a streamer page, take screenshot",
        retryAnalyzer = RetryAnalyzer.class
    )
    @Description("Full E2E: open Twitch → search StarCraft II → scroll → click streamer → assert URL and screenshot")
    @Severity(SeverityLevel.CRITICAL)
    public void should_capture_streamer_page_when_searching_starcraftII_on_mobile() {
        log.info("STEP 1 — Open Twitch home page");
        HomePage homePage = new HomePage(driver).open();
        modalHandler.dismissKeepUsingWebIfPresent();

        log.info("STEP 2 — Click search icon");
        SearchPage searchPage = homePage.clickSearchIcon();

        log.info("STEP 3 — Type 'StarCraft II'");
        searchPage.typeQuery("StarCraft II").submitSearch();

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
        modalHandler.dismissIfPresent();

        log.info("STEP 6 — Wait for full page load");
        streamerPage.waitForPageReady();

        String screenshotPath = ScreenshotUtil.capture(driver, "starcraftii_streamer");
        assertThat(screenshotPath)
                .as("Screenshot file should be created")
                .isNotNull()
                .endsWith(".png");

        assertThat(streamerPage.getCurrentUrl())
                .as("Should be on a Twitch channel page, not a search page")
                .contains("twitch.tv")
                .doesNotContain("/search");

        log.info("✓ Test passed. Screenshot: {}", screenshotPath);
    }

    // ── Test 2: data-driven search results validation ─────────────────────────

    @Test(
        description   = "Search for a game and verify Twitch navigates to the search results page",
        dataProvider  = "searchQueries",
        retryAnalyzer = RetryAnalyzer.class
    )
    @Description("Data-driven: verifies that multiple game searches navigate away from the Twitch home page")
    @Severity(SeverityLevel.NORMAL)
    public void should_show_search_results_for_game(String query) {
        log.info("STEP 1 — Open Twitch home page");
        HomePage homePage = new HomePage(driver).open();
        modalHandler.dismissKeepUsingWebIfPresent();

        log.info("STEP 2 — Search for '{}'", query);
        SearchPage searchPage = homePage.clickSearchIcon();
        searchPage.typeQuery(query).submitSearch();

        log.info("STEP 3 — Assert URL navigated away from home for '{}'", query);
        String currentUrl = driver.getCurrentUrl();
        assertThat(currentUrl)
                .as("URL after searching '%s' should leave the Twitch home page", query)
                .contains("twitch.tv")
                .doesNotMatch("https://www\\.twitch\\.tv/?$");

        String screenshotPath = ScreenshotUtil.capture(
                driver, "search_results_" + query.replace(" ", "_").toLowerCase());
        assertThat(screenshotPath)
                .as("Search results screenshot for '%s' should be created", query)
                .isNotNull()
                .endsWith(".png");

        log.info("✓ Search results loaded for '{}' — URL: {}", query, currentUrl);
    }

    // ── Teardown ───────────────────────────────────────────────────────────────

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        log.info("──── Tearing down driver ────");
        DriverFactory.quitDriver();
    }
}
