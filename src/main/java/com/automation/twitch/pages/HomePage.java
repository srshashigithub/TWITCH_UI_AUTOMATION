package com.automation.twitch.pages;

import com.automation.twitch.config.ConfigManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomePage extends BasePage {

    private static final Logger log = LoggerFactory.getLogger(HomePage.class);

    private static final By SEARCH_BUTTON       = By.xpath("//div[text()='Browse']");
    private static final By SEARCH_BUTTON_LINK  = By.xpath("//input[@placeholder='Search']");

    public HomePage(WebDriver driver) {
        super(driver);
    }

    public HomePage open() {
        String url = ConfigManager.get("base.url");
        log.info("Opening {}", url);
        driver.get(url);
        waitForDocumentReady();
        log.info("Twitch home page loaded");
        return this;
    }

    public SearchPage clickSearchIcon() {
        log.info("Clicking search icon");
        if (isPresent(SEARCH_BUTTON, 5)) {
            click(SEARCH_BUTTON);
        } else {
            click(SEARCH_BUTTON_LINK);
        }
        return new SearchPage(driver);
    }
}
