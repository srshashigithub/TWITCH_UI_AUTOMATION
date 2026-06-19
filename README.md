# Twitch UI Automation

End-to-end UI test suite for [Twitch.tv](https://www.twitch.tv) built with **Selenium 4**, **TestNG**, and the **Page Object Model** pattern. Runs Chrome in mobile-emulation mode (Nexus 5) to simulate the Twitch mobile web experience.

---

## What the test does

1. Opens `https://www.twitch.tv`
2. Dismisses the **"Keep using web"** app-redirect prompt if it appears
3. Clicks the search icon and types **StarCraft II**
4. Scrolls the results page twice
5. **Screenshots the streamer list page** → `screenshots/starcraftii_streamer_list_<timestamp>.png`
6. Clicks the first available streamer
7. Waits for the streamer page to fully load
8. **Screenshots the streamer page** → `screenshots/starcraftii_streamer_<timestamp>.png`
9. Asserts the URL is a Twitch channel page (not the search page)

---

## Project structure

```
twitch-ui-automation/
├── src/
│   ├── main/java/com/automation/twitch/
│   │   ├── config/
│   │   │   └── ConfigManager.java          # Reads config.properties
│   │   ├── driver/
│   │   │   └── DriverFactory.java          # Chrome + mobile-emulation setup
│   │   ├── pages/
│   │   │   ├── BasePage.java               # FluentWait, scroll, click helpers
│   │   │   ├── HomePage.java               # open(), clickSearchIcon()
│   │   │   ├── SearchPage.java             # typeQuery(), scrollResults(), captureListPage(), selectFirstStreamer()
│   │   │   └── StreamerPage.java           # waitForPageReady(), getCurrentUrl()
│   │   └── utils/
│   │       ├── ModalHandler.java           # Dismisses popups/overlays/banners
│   │       └── ScreenshotUtil.java         # Saves timestamped PNGs
│   └── test/
│       ├── java/com/automation/twitch/tests/
│       │   └── TwitchSearchTest.java       # Main TestNG test
│       └── resources/
│           ├── config.properties           # Browser, URL, timeout settings
│           └── logback.xml                 # Logging config
├── screenshots/                            # Auto-created at runtime (git-ignored)
├── pom.xml
├── testng.xml
└── .gitignore
```

---

## Prerequisites

| Requirement | Version |
|-------------|---------|
| Java JDK    | 17+     |
| Maven       | 3.8+    |
| Google Chrome | Latest |

> ChromeDriver is managed automatically by **WebDriverManager** — no manual driver download needed.

---

## Configuration

Edit `src/test/resources/config.properties` to change behaviour:

```properties
browser=chrome
mobile.device=Nexus 5       # Chrome DevTools mobile emulation profile

base.url=https://www.twitch.tv

page.load.timeout=60        # seconds
explicit.wait=30            # seconds
script.timeout=30           # seconds
modal.check.timeout=4       # seconds — quick popup presence check
```

---

## Running the tests

**Maven (recommended)**
```bash
mvn clean test
```

**Run a specific test class**
```bash
mvn clean test -Dtest=TwitchSearchTest
```

**Via TestNG XML directly**
```bash
mvn surefire:test
```

Screenshots are saved to the `screenshots/` folder in the project root.

---

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| selenium-java | 4.18.1 | Browser automation |
| webdrivermanager | 5.7.0 | Auto ChromeDriver management |
| testng | 7.9.0 | Test framework |
| assertj-core | 3.25.3 | Fluent assertions |
| logback-classic | 1.5.3 | Logging |
| commons-io | 2.15.1 | Screenshot file operations |

---

## Key design decisions

- **Page Object Model** — each page is a class; tests contain no raw Selenium calls
- **FluentWait in `BasePage.click()`** — retries on `StaleElementReferenceException` and `ElementClickInterceptedException`, both common in Twitch's React DOM
- **`ModalHandler`** — centralised popup handling; the "Keep using web" XPath (`//p[text()='Keep using web']`) matches Twitch's exact button text so it doesn't rely on brittle CSS class names
- **Stable XPath for streamer selection** — finds the first `<a>` whose `href` starts with `/` and is not a directory or search page, avoiding hash-generated Styled-Components class names that change on every deploy
