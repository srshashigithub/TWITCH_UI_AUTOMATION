package com.automation.twitch.utils;

import io.qameta.allure.Allure;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ScreenshotUtil {

    private static final Logger log = LoggerFactory.getLogger(ScreenshotUtil.class);
    private static final String SCREENSHOT_DIR = "screenshots";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private ScreenshotUtil() {}

    public static String capture(WebDriver driver, String label) {
        String fileName = label + "_" + LocalDateTime.now().format(TIMESTAMP) + ".png";
        String filePath = SCREENSHOT_DIR + File.separator + fileName;

        try {
            new File(SCREENSHOT_DIR).mkdirs();
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            FileUtils.writeByteArrayToFile(new File(filePath), screenshot);
            // Attach to Allure report so CI dashboards show screenshots inline
            Allure.addAttachment(label, "image/png", new ByteArrayInputStream(screenshot), "png");
            log.info("Screenshot saved → {}", filePath);
            return filePath;
        } catch (IOException e) {
            log.error("Failed to save screenshot: {}", e.getMessage());
            throw new RuntimeException("Screenshot capture failed", e);
        }
    }
}
