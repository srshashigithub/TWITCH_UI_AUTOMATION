package com.automation.twitch.driver;

import com.automation.twitch.config.ConfigManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class DriverFactory {

    private static final Logger log = LoggerFactory.getLogger(DriverFactory.class);

    // ThreadLocal allows safe parallel test execution in the future
    private static final ThreadLocal<WebDriver> driverHolder = new ThreadLocal<>();

    private DriverFactory() {}

    public static WebDriver getDriver() {
        if (driverHolder.get() == null) {
            initDriver();
        }
        return driverHolder.get();
    }

    private static void initDriver() {
        WebDriverManager.chromedriver().setup();

        String deviceName = ConfigManager.get("mobile.device");
        log.info("Initializing Chrome with mobile emulation device: '{}'", deviceName);

        Map<String, Object> mobileEmulation = new HashMap<>();
        mobileEmulation.put("deviceName", deviceName);

        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("mobileEmulation", mobileEmulation);
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--disable-infobars");
        options.addArguments("--start-maximized");

        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts()
                .pageLoadTimeout(Duration.ofSeconds(ConfigManager.getLong("page.load.timeout")))
                .scriptTimeout(Duration.ofSeconds(ConfigManager.getLong("script.timeout")));

        driverHolder.set(driver);
        log.info("Chrome driver initialized. Current URL: {}", driver.getCurrentUrl());
    }

    public static void quitDriver() {
        WebDriver driver = driverHolder.get();
        if (driver != null) {
            log.info("Closing Chrome driver");
            driver.quit();
            driverHolder.remove();
        }
    }
}
