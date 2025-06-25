# Playwright Automation Library

A comprehensive Java library that provides a simplified interface for Playwright automation testing. This library enables easy integration of Playwright into existing test frameworks and supports seamless migration from Selenium-based automation projects.

## Features

- 🌐 **Multi-Browser Support**: Chrome, Firefox, and WebKit browsers
- 🧵 **Thread-Safe**: Built-in ThreadLocal support for parallel test execution
- 🏭 **Factory Pattern**: Singleton implementation for efficient resource management
- 📱 **Cross-Platform**: Support for headless and headed browser modes
- 🔧 **Easy Integration**: Drop-in replacement for Selenium-based frameworks
- 📸 **Screenshot Support**: Built-in screenshot capture for test reporting
- ⏱️ **Smart Waits**: Configurable wait utilities for reliable automation
- 🎯 **Element Utilities**: Common element interaction methods

## Installation

### Maven Dependency

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>automation.library</groupId>
    <artifactId>playwright-automation</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle Dependency

```gradle
implementation 'automation.library:playwright-automation:1.0.0'
```

## Quick Start

### Basic Setup

```java
import automation.library.playwright.PlaywrightFactory;
import automation.library.playwright.PlaywrightUtil;
import com.microsoft.playwright.Page;

public class PlaywrightTest {
    
    @Test
    public void testExample() {
        // Initialize Playwright with Chrome browser
        PlaywrightFactory.getInstance().setDM("chrome", false); // false = headed mode
        
        // Get page instance
        Page page = PlaywrightFactory.getInstance().getPage();
        
        // Navigate to URL
        page.navigate("https://example.com");
        
        // Use utility methods
        PlaywrightUtil util = new PlaywrightUtil();
        util.waitFor(2000); // Wait for 2 seconds
        
        // Take screenshot
        util.grabScreenshot();
        
        // Clean up
        PlaywrightFactory.getInstance().closePlaywright();
    }
}
```

### Browser Configuration

```java
// Chrome (default)
PlaywrightFactory.getInstance().setDM("chrome", true); // headless mode

// Firefox
PlaywrightFactory.getInstance().setDM("firefox", false); // headed mode

// WebKit (Safari)
PlaywrightFactory.getInstance().setDM("webkit", true); // headless mode
```

## Core Components

### PlaywrightFactory
Singleton factory class for managing Playwright instances:

```java
PlaywrightFactory factory = PlaywrightFactory.getInstance();
Page page = factory.getPage();
BrowserContext context = factory.getBrowserContext();
Browser browser = factory.getBrowser();
```

### DriverContext
Thread-safe context management for parallel execution:

```java
DriverContext.getInstance().setDriverContext(testStack);
String browserName = DriverContext.getInstance().getBrowserName();
```

### PlaywrightUtil
Utility class with common automation methods:

```java
PlaywrightUtil util = new PlaywrightUtil();

// Element checks
boolean exists = util.isElementExists(locator);
boolean visible = util.isElementVisible(locator);
boolean displayed = util.isElementDisplayed(locator);

// Screenshots
util.grabScreenshot();
util.grabElementScreenshot(locator);
util.addScreenshotToReport();

// Scrolling
util.scrollToTop();
util.scrollToDown();
util.scrollToMiddle();

// Waits
util.waitFor(5000); // milliseconds
util.waitInSeconds(5); // seconds
```

## Migration from Selenium

### Before (Selenium)
```java
// Selenium setup
WebDriver driver = new ChromeDriver();
driver.get("https://example.com");
WebElement element = driver.findElement(By.id("example"));
element.click();
driver.quit();
```

### After (Playwright Library)
```java
// Playwright library setup
PlaywrightFactory.getInstance().setDM("chrome", false);
Page page = PlaywrightFactory.getInstance().getPage();
page.navigate("https://example.com");
page.locator("#example").click();
PlaywrightFactory.getInstance().closePlaywright();
```

### Migration Steps

1. **Remove Selenium Dependencies**
   ```xml
   <!-- Remove these from pom.xml -->
   <dependency>
       <groupId>org.seleniumhq.selenium</groupId>
       <artifactId>selenium-java</artifactId>
   </dependency>
   ```

2. **Add Playwright Library**
   ```xml
   <!-- Add this to pom.xml -->
   <dependency>
       <groupId>automation.library</groupId>
       <artifactId>playwright-automation</artifactId>
       <version>1.0.0</version>
   </dependency>
   ```

3. **Update Test Code**
   - Replace `WebDriver` with `Page`
   - Replace `driver.findElement()` with `page.locator()`
   - Replace `driver.get()` with `page.navigate()`
   - Use `PlaywrightFactory` for browser management

## Configuration

### System Properties

```java
// Set browser type
System.setProperty("browser", "chrome"); // chrome, firefox, webkit

// Set headless mode
System.setProperty("headless", "true");

// Set default wait duration
System.setProperty("wait.duration", "30");
```

### TestNG Integration

```java
@BeforeMethod
public void setUp() {
    String browser = System.getProperty("browser", "chrome");
    boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));
    PlaywrightFactory.getInstance().setDM(browser, headless);
}

@AfterMethod
public void tearDown() {
    PlaywrightFactory.getInstance().closePlaywright();
}
```

### TestNG Suite Example

```xml
<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">
<suite name="PlaywrightTestSuite" parallel="methods" thread-count="3">
    <parameter name="browser" value="chrome"/>
    <parameter name="headless" value="true"/>
    
    <test name="LoginTests">
        <classes>
            <class name="com.example.tests.LoginTest"/>
        </classes>
    </test>
</suite>
```

## Advanced Usage

### Parallel Execution
The library supports parallel test execution using ThreadLocal:

```java
@Test(threadPoolSize = 3, invocationCount = 5)
public void parallelTest() {
    PlaywrightFactory.getInstance().setDM("chrome", true);
    Page page = PlaywrightFactory.getInstance().getPage();
    // Test implementation
    PlaywrightFactory.getInstance().closePlaywright();
}
```

### Custom Browser Options
```java
// Extend ChromeBrowserManager for custom configurations
public class CustomChromeBrowser extends ChromeBrowserManager {
    @Override
    public void createPlaywrightDriver() {
        // Custom implementation
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
            .setHeadless(false)
            .setSlowMo(1000)
            .setArgs(Arrays.asList("--start-maximized", "--disable-web-security"));
        
        browser = playwright.chromium().launch(launchOptions);
    }
}
```

## Best Practices

1. **Always close resources**: Use `closePlaywright()` in teardown methods
2. **Use appropriate waits**: Leverage built-in wait utilities instead of Thread.sleep()
3. **Screenshot on failures**: Implement screenshot capture in exception handlers
4. **Parallel execution**: Use ThreadLocal-safe methods for concurrent testing
5. **Browser selection**: Choose appropriate browser based on test requirements

## API Reference

### PlaywrightFactory Methods
- `getInstance()` - Get singleton instance
- `setDM(String browser, boolean headless)` - Initialize browser
- `getPage()` - Get current page instance
- `getBrowser()` - Get browser instance
- `getBrowserContext()` - Get browser context
- `closePlaywright()` - Close all resources

### PlaywrightUtil Methods
- Element interaction: `isElementExists()`, `isElementVisible()`, `isElementDisplayed()`
- Screenshots: `grabScreenshot()`, `grabElementScreenshot()`, `addScreenshotToReport()`
- Navigation: `scrollToTop()`, `scrollToDown()`, `scrollToMiddle()`
- Waits: `waitFor()`, `waitInSeconds()`, `getWaitDuration()`
- Alerts: `acceptAlert()`
- App management: `launchApp()`

## Requirements

- Java 8 or higher
- Maven 3.6 or higher
- Playwright Java library dependencies

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues and questions:
- Create an issue in the repository
- Check existing documentation
- Review example implementations

---

**Happy Testing with Playwright! 🎭**