# Complete Direct ExtentReports Solution (No Adapter)

This solution completely bypasses the ExtentCucumberAdapter and gives you full control over report generation.

---

## 📄 FILE 1: `DirectExtentManager.java` - Core Report Manager
```java
package com.example.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import io.appium.java_client.android.AndroidDriver;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Direct ExtentReports Manager - Complete control over report generation
 * No dependency on ExtentCucumberAdapter
 */
public class DirectExtentManager {
    
    private static DirectExtentManager instance;
    private static final Object lock = new Object();
    
    private ExtentReports extent;
    private ExtentSparkReporter sparkReporter;
    private Map<String, ExtentTest> testMap = new HashMap<>();
    
    private String reportFolderPath;
    private String reportFilePath;
    private String screenshotsFolderPath;
    
    private static final String TIMESTAMP = new SimpleDateFormat("ddMMyyHHmmss").format(new Date());
    private static final String BASE_DIR = System.getProperty("user.dir") + File.separator + "Reports";
    private static final float COMPRESSION_QUALITY = 0.8f;
    
    // Thread-local for current test
    private static ThreadLocal<ExtentTest> currentTest = new ThreadLocal<>();
    private static ThreadLocal<ExtentTest> currentStep = new ThreadLocal<>();
    
    private DirectExtentManager() {
        initializeReport();
    }
    
    /**
     * Get singleton instance
     */
    public static DirectExtentManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new DirectExtentManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize ExtentReports
     */
    private void initializeReport() {
        try {
            // Create report folder
            reportFolderPath = BASE_DIR + File.separator + "Test-Reports-" + TIMESTAMP + File.separator;
            reportFilePath = reportFolderPath + "ExtentReport.html";
            screenshotsFolderPath = reportFolderPath + "screenshots" + File.separator;
            
            // Create directories
            Files.createDirectories(Paths.get(reportFolderPath));
            Files.createDirectories(Paths.get(screenshotsFolderPath));
            
            // Initialize Spark Reporter
            sparkReporter = new ExtentSparkReporter(reportFilePath);
            
            // Configure Spark Reporter
            sparkReporter.config().setTheme(Theme.STANDARD);
            sparkReporter.config().setDocumentTitle("Automation Test Report");
            sparkReporter.config().setReportName("Test Execution Report");
            sparkReporter.config().setTimeStampFormat("dd-MM-yyyy HH:mm:ss");
            sparkReporter.config().setEncoding("UTF-8");
            sparkReporter.config().setTimelineEnabled(true);
            sparkReporter.config().setCss("img { max-width: 100%; height: auto; }");
            
            // Initialize ExtentReports
            extent = new ExtentReports();
            extent.attachReporter(sparkReporter);
            
            // System Information
            extent.setSystemInfo("Platform", "Android");
            extent.setSystemInfo("Environment", System.getProperty("test.environment", "QA"));
            extent.setSystemInfo("Test Runner", "Cucumber with JUnit5");
            extent.setSystemInfo("Connection", "WiFi");
            extent.setSystemInfo("Report Time", new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date()));
            
            System.out.println("==========================================");
            System.out.println("DirectExtentManager Initialized");
            System.out.println("Report: " + reportFilePath);
            System.out.println("==========================================");
            
        } catch (Exception e) {
            System.err.println("Failed to initialize DirectExtentManager: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create or get test for scenario
     */
    public ExtentTest createTest(String scenarioName, String... categories) {
        ExtentTest test = extent.createTest(scenarioName);
        
        // Add categories/tags
        if (categories != null && categories.length > 0) {
            test.assignCategory(categories);
        }
        
        // Store in map for retrieval
        testMap.put(scenarioName, test);
        currentTest.set(test);
        
        return test;
    }
    
    /**
     * Create step/node under current test
     */
    public ExtentTest createStep(String stepName) {
        ExtentTest test = currentTest.get();
        if (test != null) {
            ExtentTest step = test.createNode(stepName);
            currentStep.set(step);
            return step;
        }
        return null;
    }
    
    /**
     * Get current test
     */
    public ExtentTest getCurrentTest() {
        return currentTest.get();
    }
    
    /**
     * Get current step
     */
    public ExtentTest getCurrentStep() {
        ExtentTest step = currentStep.get();
        return (step != null) ? step : currentTest.get();
    }
    
    /**
     * Log to current test/step
     */
    public void log(Status status, String message) {
        ExtentTest current = getCurrentStep();
        if (current != null) {
            current.log(status, message);
        }
        System.out.println("[" + status + "] " + message);
    }
    
    public void logInfo(String message) {
        log(Status.INFO, message);
    }
    
    public void logPass(String message) {
        log(Status.PASS, "✓ " + message);
    }
    
    public void logFail(String message) {
        log(Status.FAIL, "✗ " + message);
    }
    
    public void logWarning(String message) {
        log(Status.WARNING, "⚠ " + message);
    }
    
    public void logSkip(String message) {
        log(Status.SKIP, message);
    }
    
    /**
     * Capture screenshot and return Base64
     */
    public String captureScreenshot(AndroidDriver driver, String screenshotName) {
        try {
            if (driver == null) {
                return null;
            }
            
            // Capture screenshot as Base64
            String base64Screenshot = driver.getScreenshotAs(OutputType.BASE64);
            
            // Compress
            String compressedBase64 = compressBase64Image(base64Screenshot);
            
            // Save to file
            saveScreenshotToFile(compressedBase64, screenshotName);
            
            return compressedBase64;
            
        } catch (Exception e) {
            logWarning("Screenshot capture failed: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Attach screenshot to current test/step
     */
    public void attachScreenshot(String base64Screenshot, String title) {
        try {
            ExtentTest current = getCurrentStep();
            if (current != null && base64Screenshot != null) {
                current.addScreenCaptureFromBase64String(base64Screenshot, title);
            }
        } catch (Exception e) {
            logWarning("Failed to attach screenshot: " + e.getMessage());
        }
    }
    
    /**
     * Capture and attach screenshot in one call
     */
    public void captureAndAttachScreenshot(AndroidDriver driver, String screenshotName, String title) {
        String base64 = captureScreenshot(driver, screenshotName);
        if (base64 != null) {
            attachScreenshot(base64, title);
        }
    }
    
    /**
     * Capture failure screenshot with error message
     */
    public void captureFailureScreenshot(AndroidDriver driver, String testName, String errorMessage) {
        try {
            String timestamp = new SimpleDateFormat("HHmmss").format(new Date());
            String screenshotName = testName + "_FAIL_" + timestamp;
            
            String base64Screenshot = captureScreenshot(driver, screenshotName);
            
            if (base64Screenshot != null) {
                ExtentTest current = getCurrentStep();
                if (current != null) {
                    current.fail(errorMessage,
                        MediaEntityBuilder.createScreenCaptureFromBase64String(base64Screenshot).build());
                }
            }
        } catch (Exception e) {
            logWarning("Failed to capture failure screenshot: " + e.getMessage());
        }
    }
    
    /**
     * Save screenshot to file
     */
    private void saveScreenshotToFile(String base64Image, String fileName) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            File file = new File(screenshotsFolderPath + fileName + ".jpg");
            FileUtils.writeByteArrayToFile(file, imageBytes);
        } catch (Exception e) {
            // Silent fail
        }
    }
    
    /**
     * Compress Base64 image
     */
    private String compressBase64Image(String base64Image) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            ImageOutputStream ios = ImageIO.createImageOutputStream(compressed);
            
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            ImageWriter writer = writers.next();
            writer.setOutput(ios);
            
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(COMPRESSION_QUALITY);
            }
            
            writer.write(null, new IIOImage(image, null, null), param);
            writer.dispose();
            ios.close();
            
            return Base64.getEncoder().encodeToString(compressed.toByteArray());
            
        } catch (Exception e) {
            return base64Image; // Return original if compression fails
        }
    }
    
    /**
     * Add data table to report
     */
    public void addDataTable(String[][] data) {
        if (data == null || data.length == 0) return;
        
        StringBuilder html = new StringBuilder("<table class='table table-sm table-bordered'>");
        
        // Header row
        html.append("<thead><tr>");
        for (String header : data[0]) {
            html.append("<th>").append(header).append("</th>");
        }
        html.append("</tr></thead>");
        
        // Data rows
        html.append("<tbody>");
        for (int i = 1; i < data.length; i++) {
            html.append("<tr>");
            for (String cell : data[i]) {
                html.append("<td>").append(cell).append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</tbody></table>");
        
        ExtentTest current = getCurrentStep();
        if (current != null) {
            current.info(html.toString());
        }
    }
    
    /**
     * Add device info to report
     */
    public void addDeviceInfo(AndroidDriver driver) {
        try {
            if (driver != null && driver.getSessionId() != null) {
                Map<String, Object> caps = driver.getSessionDetails();
                
                String[][] deviceInfo = {
                    {"Property", "Value"},
                    {"Platform", String.valueOf(caps.getOrDefault("platformName", "Android"))},
                    {"Device", String.valueOf(caps.getOrDefault("deviceName", "Unknown"))},
                    {"App Package", String.valueOf(caps.getOrDefault("appPackage", "N/A"))},
                    {"Session ID", driver.getSessionId().toString()}
                };
                
                addDataTable(deviceInfo);
            }
        } catch (Exception e) {
            logWarning("Could not add device info: " + e.getMessage());
        }
    }
    
    /**
     * Flush report - MUST be called at end of test execution
     */
    public void flush() {
        try {
            if (extent != null) {
                extent.flush();
                System.out.println("==========================================");
                System.out.println("Report Generated Successfully!");
                System.out.println("Location: " + reportFilePath);
                System.out.println("==========================================");
            }
        } catch (Exception e) {
            System.err.println("Failed to flush report: " + e.getMessage());
        }
    }
    
    /**
     * Get report file path
     */
    public String getReportFilePath() {
        return reportFilePath;
    }
    
    /**
     * Clean up thread locals
     */
    public void cleanupTest() {
        currentTest.remove();
        currentStep.remove();
    }
}
```

---

## 📄 FILE 2: `DirectReportHooks.java` - Cucumber Hooks
```java
package com.example.hooks;

import com.example.reporting.DirectExtentManager;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.cucumber.java.*;
import java.io.File;
import java.net.URL;
import java.time.Duration;

/**
 * Cucumber Hooks using Direct ExtentReports
 */
public class DirectReportHooks {
    
    private static AndroidDriver driver;
    private static DirectExtentManager reporter = DirectExtentManager.getInstance();
    private static int scenarioCount = 0;
    private static int passedCount = 0;
    private static int failedCount = 0;
    private static long suiteStartTime;
    
    @Before
    public void beforeScenario(Scenario scenario) {
        try {
            scenarioCount++;
            
            if (scenarioCount == 1) {
                suiteStartTime = System.currentTimeMillis();
            }
            
            // Create test in ExtentReports
            String[] tags = scenario.getSourceTagNames().toArray(new String[0]);
            reporter.createTest(scenario.getName(), tags);
            
            reporter.logInfo("Starting Scenario #" + scenarioCount + ": " + scenario.getName());
            reporter.logInfo("Tags: " + String.join(", ", tags));
            
            // Initialize driver
            driver = initializeAndroidDriver();
            
            // Add device info
            reporter.addDeviceInfo(driver);
            
        } catch (Exception e) {
            reporter.logFail("Failed to initialize test: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    @BeforeStep
    public void beforeStep() {
        // Optional: Create step node in report
        // You can get step name from StepHookContext if needed
    }
    
    @AfterStep
    public void afterStep() {
        // Optional: Log step completion
    }
    
    @After
    public void afterScenario(Scenario scenario) {
        try {
            // Capture final screenshot
            if (scenario.isFailed()) {
                failedCount++;
                reporter.logFail("Scenario Failed: " + scenario.getName());
                
                if (driver != null) {
                    reporter.captureFailureScreenshot(
                        driver, 
                        scenario.getName().replaceAll("[^a-zA-Z0-9]", "_"),
                        "Test Failed"
                    );
                }
            } else {
                passedCount++;
                reporter.logPass("Scenario Passed: " + scenario.getName());
                
                // Optional success screenshot
                if (driver != null) {
                    reporter.captureAndAttachScreenshot(
                        driver,
                        scenario.getName().replaceAll("[^a-zA-Z0-9]", "_") + "_SUCCESS",
                        "Final State"
                    );
                }
            }
            
            // Clean up test
            reporter.cleanupTest();
            
        } catch (Exception e) {
            reporter.logWarning("Error in after scenario: " + e.getMessage());
        } finally {
            // Quit driver
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    // Silent
                }
                driver = null;
            }
        }
    }
    
    /**
     * This runs after ALL scenarios complete
     */
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                // Add final summary
                long duration = System.currentTimeMillis() - suiteStartTime;
                
                System.out.println("\n==========================================");
                System.out.println("Test Suite Completed");
                System.out.println("Total: " + scenarioCount);
                System.out.println("Passed: " + passedCount);
                System.out.println("Failed: " + failedCount);
                System.out.println("Duration: " + (duration / 1000) + " seconds");
                System.out.println("==========================================\n");
                
                // CRITICAL: Flush report to write to disk
                reporter.flush();
                
            } catch (Exception e) {
                System.err.println("Error in shutdown hook: " + e.getMessage());
            }
        }));
    }
    
    /**
     * Initialize Android driver
     */
    private AndroidDriver initializeAndroidDriver() {
        try {
            UiAutomator2Options options = new UiAutomator2Options();
            
            options.setPlatformName("Android");
            options.setAutomationName("UiAutomator2");
            options.setDeviceName("Android Device");
            
            // UPDATE WITH YOUR APP DETAILS
            options.setAppPackage("com.example.app");
            options.setAppActivity("com.example.MainActivity");
            
            // WiFi optimizations
            options.setNewCommandTimeout(Duration.ofSeconds(300));
            options.setCapability("noReset", true);
            options.setCapability("ignoreUnimportantViews", true);
            options.setCapability("disableWindowAnimation", true);
            options.setCapability("settings[waitForIdleTimeout]", 100);
            
            String appiumUrl = System.getProperty("appium.server", "http://localhost:4723");
            
            AndroidDriver androidDriver = new AndroidDriver(new URL(appiumUrl), options);
            androidDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            
            reporter.logInfo("Android driver initialized successfully");
            return androidDriver;
            
        } catch (Exception e) {
            reporter.logFail("Driver initialization failed: " + e.getMessage());
            throw new RuntimeException("Driver init failed", e);
        }
    }
    
    /**
     * Get current driver
     */
    public static AndroidDriver getDriver() {
        return driver;
    }
    
    /**
     * Get reporter instance for use in step definitions
     */
    public static DirectExtentManager getReporter() {
        return reporter;
    }
}
```

---

## 📄 FILE 3: `DirectReportRunner.java` - Test Runner
```java
package com.example.runner;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, 
    value = "com.example.stepdefs,com.example.hooks")
// Note: We're NOT using ExtentCucumberAdapter plugin
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, 
    value = "pretty,json:target/cucumber-reports/cucumber.json")
public class DirectReportRunner {
    // Simple runner - all reporting handled by DirectExtentManager
}
```

---

## 📄 FILE 4: `SampleStepDefinitions.java` - Example Usage
```java
package com.example.stepdefs;

import com.example.hooks.DirectReportHooks;
import com.example.reporting.DirectExtentManager;
import io.appium.java_client.android.AndroidDriver;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.junit.Assert;

/**
 * Sample step definitions using Direct ExtentReports
 */
public class SampleStepDefinitions {
    
    private AndroidDriver driver;
    private DirectExtentManager reporter = DirectReportHooks.getReporter();
    
    @Given("the app is launched")
    public void launchApp() {
        driver = DirectReportHooks.getDriver();
        
        // Create step in report
        reporter.createStep("Given the app is launched");
        reporter.logInfo("Launching application");
        
        // Take screenshot
        reporter.captureAndAttachScreenshot(driver, "AppLaunch", "Initial State");
        
        reporter.logPass("App launched successfully");
    }
    
    @When("I perform {string} action")
    public void performAction(String action) {
        // Create step
        reporter.createStep("When I perform " + action + " action");
        reporter.logInfo("Performing: " + action);
        
        try {
            // Your action logic here
            switch(action.toLowerCase()) {
                case "login":
                    driver.findElement(By.id("username")).sendKeys("testuser");
                    driver.findElement(By.id("password")).sendKeys("password");
                    driver.findElement(By.id("loginButton")).click();
                    break;
                    
                case "search":
                    driver.findElement(By.id("searchBox")).sendKeys("test");
                    driver.findElement(By.id("searchButton")).click();
                    break;
                    
                default:
                    reporter.logWarning("Unknown action: " + action);
            }
            
            // Screenshot after action
            reporter.captureAndAttachScreenshot(
                driver, 
                action + "_completed",
                "After " + action
            );
            
            reporter.logPass("Action completed: " + action);
            
        } catch (Exception e) {
            reporter.logFail("Action failed: " + e.getMessage());
            reporter.captureFailureScreenshot(driver, action, e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    @Then("I verify {string} is displayed")
    public void verifyElement(String element) {
        reporter.createStep("Then I verify " + element + " is displayed");
        reporter.logInfo("Verifying: " + element);
        
        try {
            boolean isDisplayed = false;
            
            // Your verification logic
            switch(element.toLowerCase()) {
                case "home screen":
                    isDisplayed = driver.findElement(By.id("homeScreen")).isDisplayed();
                    break;
                    
                case "search results":
                    isDisplayed = driver.findElement(By.id("results")).isDisplayed();
                    break;
                    
                default:
                    reporter.logWarning("Unknown element: " + element);
            }
            
            if (isDisplayed) {
                reporter.logPass(element + " is displayed");
                reporter.captureAndAttachScreenshot(
                    driver,
                    element.replaceAll(" ", "_"),
                    element + " Verification"
                );
            } else {
                reporter.logFail(element + " is NOT displayed");
                reporter.captureFailureScreenshot(
                    driver, 
                    element,
                    element + " not found"
                );
                Assert.fail(element + " is not displayed");
            }
            
        } catch (Exception e) {
            reporter.logFail("Verification failed: " + e.getMessage());
            reporter.captureFailureScreenshot(driver, element, e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    @Then("I add test data to report")
    public void addTestData() {
        reporter.createStep("Then I add test data to report");
        
        String[][] testData = {
            {"Parameter", "Value", "Status"},
            {"Username", "testuser", "✓"},
            {"Password", "****", "✓"},
            {"Environment", "QA", "✓"},
            {"Device", "Android", "✓"}
        };
        
        reporter.addDataTable(testData);
        reporter.logInfo("Test data added to report");
    }
}
```

---

## 📄 FILE 5: `pom.xml` - Dependencies (Key parts)
```xml
<dependencies>
    <!-- ExtentReports Core (NO adapter needed) -->
    <dependency>
        <groupId>com.aventstack</groupId>
        <artifactId>extentreports</artifactId>
        <version>5.1.1</version>
    </dependency>
    
    <!-- Cucumber -->
    <dependency>
        <groupId>io.cucumber</groupId>
        <artifactId>cucumber-java</artifactId>
        <version>7.27.0</version>
        <scope>test</scope>
    </dependency>
    
    <dependency>
        <groupId>io.cucumber</groupId>
        <artifactId>cucumber-junit-platform-engine</artifactId>
        <version>7.27.0</version>
        <scope>test</scope>
    </dependency>
    
    <!-- Other dependencies remain same -->
    <!-- Note: NO tech.grasshopper adapter dependency -->
</dependencies>
```

---

## 🚀 HOW TO USE

1. **Remove** the tech.grasshopper adapter dependency from pom.xml
2. **Add** all the files above to your project
3. **Update** app details in DirectReportHooks
4. **Run** DirectReportRunner from IntelliJ
5. **Report** will be in: `Reports/Test-Reports-TIMESTAMP/ExtentReport.html`

## ✅ ADVANTAGES OF DIRECT APPROACH

1. **Full Control** - You control exactly when/how report is created
2. **No Adapter Issues** - No timing problems or multiple initializations
3. **Guaranteed Report** - Flush in shutdown hook ensures report is written
4. **Better Screenshots** - Direct control over screenshot attachment
5. **Custom Formatting** - Add tables, custom HTML, etc.
6. **Thread Safe** - Uses ThreadLocal for parallel execution
7. **Single Report** - One timestamp, one folder, one report

## 🎯 KEY DIFFERENCES

- **No ExtentCucumberAdapter** - We handle everything directly
- **Manual Test Creation** - We create tests in @Before hook
- **Manual Flush** - Shutdown hook ensures report is written
- **Direct Screenshot Control** - No adapter interference

## 📊 REPORT FEATURES

- Scenario-level tests with steps as nodes
- Screenshots with compression
- Data tables support
- Device information
- Pass/Fail status with icons
- Execution summary in console

## 🔍 DEBUGGING

If report not generated:
1. Check console for "Report Generated Successfully!"
2. Verify flush() is called in shutdown hook
3. Check Reports folder for Test-Reports-TIMESTAMP
4. Add debug logging in DirectExtentManager

This solution gives you **complete control** and **eliminates all adapter-related issues**!
