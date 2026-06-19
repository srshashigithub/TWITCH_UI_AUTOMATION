package com.automation.twitch.driver;

import com.automation.twitch.config.ConfigManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DriverFactory {

    private static final Logger log = LoggerFactory.getLogger(DriverFactory.class);

    // ThreadLocal gives each thread its own WebDriver — safe for parallel test execution
    private static final ThreadLocal<WebDriver> driverHolder = new ThreadLocal<>();

    private DriverFactory() {}

    public static WebDriver getDriver() {
        if (driverHolder.get() == null) {
            initDriver();
        }
        return driverHolder.get();
    }

    private static void initDriver() {
        String browser    = ConfigManager.get("browser").toLowerCase();
        String deviceName = ConfigManager.get("mobile.device");
        log.info("Initializing '{}' driver with mobile emulation device: '{}'", browser, deviceName);

        Map<String, Object> mobileEmulation = new HashMap<>();
        mobileEmulation.put("deviceName", deviceName);

        WebDriver driver = switch (browser) {
            case "firefox" -> {
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions opts = new FirefoxOptions();
                // Firefox has no Chrome mobileEmulation API — override UA to simulate Nexus 5
                opts.addPreference("general.useragent.override",
                        "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36");
                yield new FirefoxDriver(opts);
            }
            case "edge" -> {
                WebDriverManager.edgedriver().setup();
                EdgeOptions opts = new EdgeOptions();
                opts.setExperimentalOption("mobileEmulation", mobileEmulation);
                opts.addArguments("--no-sandbox", "--disable-dev-shm-usage",
                        "--disable-notifications", "--disable-popup-blocking",
                        "--disable-infobars", "--start-maximized");
                yield new EdgeDriver(opts);
            }
            default -> { // chrome
                WebDriverManager.chromedriver().setup();
                yield new ChromeDriver(buildChromeOptions(mobileEmulation));
            }
        };

        driver.manage().timeouts()
                .pageLoadTimeout(Duration.ofSeconds(ConfigManager.getLong("page.load.timeout")))
                .scriptTimeout(Duration.ofSeconds(ConfigManager.getLong("script.timeout")));

        driverHolder.set(driver);
        log.info("{} driver initialized. Current URL: {}", driver.getClass().getSimpleName(), driver.getCurrentUrl());
    }

    private static ChromeOptions buildChromeOptions(Map<String, Object> mobileEmulation) {
        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("mobileEmulation", mobileEmulation);
        options.addArguments(
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-notifications",
                "--disable-popup-blocking",
                "--disable-infobars",
                "--start-maximized"
        );
        return options;
    }

    public static void quitDriver() {
        WebDriver driver = driverHolder.get();
        if (driver != null) {
            log.info("Closing {} driver", driver.getClass().getSimpleName());
            driver.quit();
            driverHolder.remove();
        }
    }
}
