# Automated Direct ExtentReports - No Manual Step Creation!

This solution automatically captures ALL Cucumber steps using a custom plugin while still avoiding adapter issues.

---

## 📄 FILE 1: `AutoExtentReportPlugin.java` - Custom Cucumber Plugin
```java
package com.example.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom Cucumber Plugin that automatically captures all steps
 * No manual step creation needed!
 */
public class AutoExtentReportPlugin implements ConcurrentEventListener {
    
    private static ExtentReports extent;
    private static Map<String, ExtentTest> featureMap = new HashMap<>();
    private static ThreadLocal<ExtentTest> scenarioTest = new ThreadLocal<>();
    private static ThreadLocal<ExtentTest> stepTest = new ThreadLocal<>();
    
    private static final String TIMESTAMP = new SimpleDateFormat("ddMMyyHHmmss").format(new Date());
    private static final String BASE_DIR = System.getProperty("user.dir") + File.separator + "Reports";
    private static String reportPath;
    
    static {
        initializeReport();
    }
    
    private static void initializeReport() {
        try {
            // Create report folder
            String reportFolder = BASE_DIR + File.separator + "Test-Reports-" + TIMESTAMP + File.separator;
            reportPath = reportFolder + "ExtentReport.html";
            
            Files.createDirectories(Paths.get(reportFolder));
            Files.createDirectories(Paths.get(reportFolder + "screenshots"));
            
            // Initialize ExtentReports
            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
            spark.config().setTheme(Theme.STANDARD);
            spark.config().setDocumentTitle("Automation Test Report");
            spark.config().setReportName("Cucumber Test Report");
            spark.config().setTimeStampFormat("dd-MM-yyyy HH:mm:ss");
            
            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("Platform", "Android");
            extent.setSystemInfo("Environment", "QA");
            
            // Make extent accessible to DirectExtentManager
            DirectExtentManager.setExtentReports(extent);
            
            System.out.println("==========================================");
            System.out.println("AutoExtentReportPlugin Initialized");
            System.out.println("Report: " + reportPath);
            System.out.println("==========================================");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void setEventPublisher(EventPublisher publisher) {
        // Test Run Started
        publisher.registerHandlerFor(TestRunStarted.class, this::handleTestRunStarted);
        
        // Feature
        publisher.registerHandlerFor(TestCaseStarted.class, this::handleTestCaseStarted);
        
        // Scenario
        publisher.registerHandlerFor(TestCaseFinished.class, this::handleTestCaseFinished);
        
        // Steps
        publisher.registerHandlerFor(TestStepStarted.class, this::handleTestStepStarted);
        publisher.registerHandlerFor(TestStepFinished.class, this::handleTestStepFinished);
        
        // Test Run Finished
        publisher.registerHandlerFor(TestRunFinished.class, this::handleTestRunFinished);
    }
    
    private void handleTestRunStarted(TestRunStarted event) {
        System.out.println("Test Run Started at: " + new Date());
    }
    
    private void handleTestCaseStarted(TestCaseStarted event) {
        String scenarioName = event.getTestCase().getName();
        String featureName = getFeatureName(event.getTestCase().getUri());
        
        // Create or get feature test
        ExtentTest feature = featureMap.computeIfAbsent(featureName, 
            name -> extent.createTest("Feature: " + name));
        
        // Create scenario under feature
        ExtentTest scenario = feature.createNode("Scenario: " + scenarioName);
        scenarioTest.set(scenario);
        
        // Make available to DirectExtentManager
        DirectExtentManager.setCurrentTest(scenario);
    }
    
    private void handleTestStepStarted(TestStepStarted event) {
        if (event.getTestStep() instanceof PickleStepTestStep) {
            PickleStepTestStep step = (PickleStepTestStep) event.getTestStep();
            String stepText = step.getStep().getKeyword() + step.getStep().getText();
            
            ExtentTest scenario = scenarioTest.get();
            if (scenario != null) {
                ExtentTest stepNode = scenario.createNode(stepText);
                stepTest.set(stepNode);
                
                // Make available to DirectExtentManager
                DirectExtentManager.setCurrentStep(stepNode);
            }
        }
    }
    
    private void handleTestStepFinished(TestStepFinished event) {
        if (event.getTestStep() instanceof PickleStepTestStep) {
            ExtentTest step = stepTest.get();
            if (step != null) {
                Result result = event.getResult();
                Status status = mapStatus(result.getStatus());
                
                if (result.getError() != null) {
                    step.log(status, "Step failed: " + result.getError().getMessage());
                } else {
                    step.log(status, "Step " + status.toString().toLowerCase());
                }
            }
        }
    }
    
    private void handleTestCaseFinished(TestCaseFinished event) {
        ExtentTest scenario = scenarioTest.get();
        if (scenario != null) {
            Result result = event.getResult();
            Status status = mapStatus(result.getStatus());
            
            if (result.getError() != null) {
                scenario.log(status, "Scenario failed: " + result.getError().getMessage());
            }
        }
        
        // Clean up
        scenarioTest.remove();
        stepTest.remove();
    }
    
    private void handleTestRunFinished(TestRunFinished event) {
        extent.flush();
        System.out.println("==========================================");
        System.out.println("Report Generated: " + reportPath);
        System.out.println("==========================================");
    }
    
    private Status mapStatus(io.cucumber.plugin.event.Status cucumberStatus) {
        switch (cucumberStatus) {
            case PASSED:
                return Status.PASS;
            case FAILED:
                return Status.FAIL;
            case SKIPPED:
            case PENDING:
                return Status.SKIP;
            case AMBIGUOUS:
            case UNDEFINED:
                return Status.WARNING;
            default:
                return Status.INFO;
        }
    }
    
    private String getFeatureName(java.net.URI uri) {
        String path = uri.getPath();
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        return fileName.replace(".feature", "");
    }
}
```

---

## 📄 FILE 2: `DirectExtentManager.java` - Simplified Manager
```java
package com.example.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import io.appium.java_client.android.AndroidDriver;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Simplified DirectExtentManager - works with AutoExtentReportPlugin
 * No need to manually create steps!
 */
public class DirectExtentManager {
    
    private static DirectExtentManager instance = new DirectExtentManager();
    private static ExtentReports extent;
    private static ThreadLocal<ExtentTest> currentTest = new ThreadLocal<>();
    private static ThreadLocal<ExtentTest> currentStep = new ThreadLocal<>();
    
    private static final float COMPRESSION_QUALITY = 0.8f;
    private static final String TIMESTAMP = new SimpleDateFormat("ddMMyyHHmmss").format(new Date());
    
    private DirectExtentManager() {
        // Private constructor
    }
    
    public static DirectExtentManager getInstance() {
        return instance;
    }
    
    /**
     * Set ExtentReports instance from plugin
     */
    public static void setExtentReports(ExtentReports extentReports) {
        extent = extentReports;
    }
    
    /**
     * Set current test from plugin
     */
    public static void setCurrentTest(ExtentTest test) {
        currentTest.set(test);
    }
    
    /**
     * Set current step from plugin
     */
    public static void setCurrentStep(ExtentTest step) {
        currentStep.set(step);
    }
    
    /**
     * Get current context (step or test)
     */
    private ExtentTest getCurrentContext() {
        ExtentTest step = currentStep.get();
        if (step != null) {
            return step;
        }
        return currentTest.get();
    }
    
    /**
     * Simple logging methods - no need to create steps!
     */
    public void logInfo(String message) {
        ExtentTest context = getCurrentContext();
        if (context != null) {
            context.info(message);
        }
        System.out.println("[INFO] " + message);
    }
    
    public void logPass(String message) {
        ExtentTest context = getCurrentContext();
        if (context != null) {
            context.pass("✓ " + message);
        }
        System.out.println("[PASS] " + message);
    }
    
    public void logFail(String message) {
        ExtentTest context = getCurrentContext();
        if (context != null) {
            context.fail("✗ " + message);
        }
        System.out.println("[FAIL] " + message);
    }
    
    public void logWarning(String message) {
        ExtentTest context = getCurrentContext();
        if (context != null) {
            context.warning("⚠ " + message);
        }
        System.out.println("[WARN] " + message);
    }
    
    /**
     * Capture and attach screenshot
     */
    public void captureAndAttachScreenshot(AndroidDriver driver, String screenshotName, String title) {
        try {
            if (driver == null) return;
            
            String base64 = driver.getScreenshotAs(OutputType.BASE64);
            String compressed = compressBase64Image(base64);
            
            ExtentTest context = getCurrentContext();
            if (context != null && compressed != null) {
                context.addScreenCaptureFromBase64String(compressed, title);
            }
            
            // Save to file
            saveScreenshotToFile(compressed, screenshotName);
            
        } catch (Exception e) {
            logWarning("Screenshot failed: " + e.getMessage());
        }
    }
    
    /**
     * Capture failure screenshot
     */
    public void captureFailureScreenshot(AndroidDriver driver, String testName) {
        try {
            if (driver == null) return;
            
            String timestamp = new SimpleDateFormat("HHmmss").format(new Date());
            String screenshotName = testName + "_FAIL_" + timestamp;
            
            String base64 = driver.getScreenshotAs(OutputType.BASE64);
            String compressed = compressBase64Image(base64);
            
            ExtentTest context = getCurrentContext();
            if (context != null && compressed != null) {
                context.fail("Test Failed",
                    MediaEntityBuilder.createScreenCaptureFromBase64String(compressed).build());
            }
            
            saveScreenshotToFile(compressed, screenshotName);
            
        } catch (Exception e) {
            logWarning("Failure screenshot failed: " + e.getMessage());
        }
    }
    
    /**
     * Add data table
     */
    public void addDataTable(String[][] data) {
        if (data == null || data.length == 0) return;
        
        StringBuilder html = new StringBuilder("<table class='table table-sm'>");
        html.append("<thead><tr>");
        for (String header : data[0]) {
            html.append("<th>").append(header).append("</th>");
        }
        html.append("</tr></thead><tbody>");
        
        for (int i = 1; i < data.length; i++) {
            html.append("<tr>");
            for (String cell : data[i]) {
                html.append("<td>").append(cell).append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</tbody></table>");
        
        ExtentTest context = getCurrentContext();
        if (context != null) {
            context.info(html.toString());
        }
    }
    
    private void saveScreenshotToFile(String base64Image, String fileName) {
        try {
            String screenshotPath = System.getProperty("user.dir") + File.separator + 
                                   "Reports" + File.separator + 
                                   "Test-Reports-" + TIMESTAMP + File.separator + 
                                   "screenshots" + File.separator;
            
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            File file = new File(screenshotPath + fileName + ".jpg");
            FileUtils.writeByteArrayToFile(file, imageBytes);
        } catch (Exception e) {
            // Silent
        }
    }
    
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
            return base64Image;
        }
    }
}
```

---

## 📄 FILE 3: `SimpleCucumberHooks.java` - Clean Hooks
```java
package com.example.hooks;

import com.example.reporting.DirectExtentManager;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.cucumber.java.*;
import java.net.URL;
import java.time.Duration;

/**
 * Simple hooks - no need to create tests/steps manually!
 */
public class SimpleCucumberHooks {
    
    private static AndroidDriver driver;
    private static DirectExtentManager reporter = DirectExtentManager.getInstance();
    
    @Before
    public void beforeScenario(Scenario scenario) {
        try {
            // Just initialize driver - plugin handles test creation
            driver = initializeAndroidDriver();
            reporter.logInfo("Driver initialized for: " + scenario.getName());
            
        } catch (Exception e) {
            reporter.logFail("Failed to initialize: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    @After
    public void afterScenario(Scenario scenario) {
        try {
            if (scenario.isFailed()) {
                reporter.captureFailureScreenshot(driver, 
                    scenario.getName().replaceAll("[^a-zA-Z0-9]", "_"));
            }
        } finally {
            if (driver != null) {
                driver.quit();
                driver = null;
            }
        }
    }
    
    private AndroidDriver initializeAndroidDriver() {
        try {
            UiAutomator2Options options = new UiAutomator2Options();
            
            options.setPlatformName("Android");
            options.setAutomationName("UiAutomator2");
            options.setDeviceName("Android Device");
            
            // UPDATE YOUR APP DETAILS
            options.setAppPackage("com.example.app");
            options.setAppActivity("com.example.MainActivity");
            
            // WiFi optimizations
            options.setNewCommandTimeout(Duration.ofSeconds(300));
            options.setCapability("noReset", true);
            options.setCapability("ignoreUnimportantViews", true);
            options.setCapability("disableWindowAnimation", true);
            
            String appiumUrl = System.getProperty("appium.server", "http://localhost:4723");
            return new AndroidDriver(new URL(appiumUrl), options);
            
        } catch (Exception e) {
            throw new RuntimeException("Driver init failed", e);
        }
    }
    
    public static AndroidDriver getDriver() {
        return driver;
    }
}
```

---

## 📄 FILE 4: `SimpleStepDefinitions.java` - Clean Step Defs
```java
package com.example.stepdefs;

import com.example.hooks.SimpleCucumberHooks;
import com.example.reporting.DirectExtentManager;
import io.appium.java_client.android.AndroidDriver;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.junit.Assert;

/**
 * Simple step definitions - NO manual step creation needed!
 * Plugin automatically captures all steps
 */
public class SimpleStepDefinitions {
    
    private AndroidDriver driver;
    private DirectExtentManager reporter = DirectExtentManager.getInstance();
    
    @Given("the app is launched")
    public void launchApp() {
        driver = SimpleCucumberHooks.getDriver();
        
        // Just log and take screenshots - no need to create step!
        reporter.logInfo("Launching application");
        reporter.captureAndAttachScreenshot(driver, "AppLaunch", "Initial State");
        reporter.logPass("App launched successfully");
    }
    
    @When("I perform {string} action")
    public void performAction(String action) {
        // No manual step creation - just use reporter!
        reporter.logInfo("Performing: " + action);
        
        try {
            // Your action logic
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
            }
            
            reporter.captureAndAttachScreenshot(driver, action, "After " + action);
            reporter.logPass("Action completed");
            
        } catch (Exception e) {
            reporter.logFail("Action failed: " + e.getMessage());
            reporter.captureFailureScreenshot(driver, action);
            throw e;
        }
    }
    
    @Then("I verify {string} is displayed")
    public void verifyElement(String element) {
        // Just log - plugin handles the step!
        reporter.logInfo("Verifying: " + element);
        
        boolean isDisplayed = driver.findElement(By.id("someId")).isDisplayed();
        
        if (isDisplayed) {
            reporter.logPass(element + " is displayed");
            reporter.captureAndAttachScreenshot(driver, element, "Verification");
        } else {
            reporter.logFail(element + " is NOT displayed");
            reporter.captureFailureScreenshot(driver, element);
            Assert.fail(element + " not found");
        }
    }
}
```

---

## 📄 FILE 5: `SimpleRunner.java` - Test Runner with Plugin
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
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, 
    value = "com.example.reporting.AutoExtentReportPlugin")  // Our custom plugin!
public class SimpleRunner {
    // That's it! Plugin handles everything
}
```

---

## 🎯 BENEFITS

1. **Automatic Step Capture** - Plugin captures ALL steps automatically
2. **No Manual Creation** - No need for `createStep()` calls
3. **Clean Code** - Step definitions stay clean and simple
4. **Reliable Report** - Single folder, guaranteed generation
5. **Full Control** - Still have all screenshot/logging capabilities

## 🚀 HOW TO USE

1. Add all files to your project
2. Update app details in SimpleCucumberHooks
3. Run SimpleRunner from IntelliJ
4. Report appears in: `Reports/Test-Reports-TIMESTAMP/ExtentReport.html`

## ✨ THE MAGIC

The `AutoExtentReportPlugin` listens to Cucumber events and automatically:
- Creates features and scenarios in the report
- Captures every step with its status
- Makes the current test/step available to DirectExtentManager
- Flushes the report at the end

You just write normal step definitions and use the reporter for screenshots/logging - no manual step creation needed!
